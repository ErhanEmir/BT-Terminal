package com.erhanemir.btterminal

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.os.Looper
import java.io.IOException
import java.util.UUID

/** HC-05 / HC-06 (SPP) baglanti yoneticisi. Tum islemler arka planda, callback'ler UI thread'de. */
object BtManager {

    private val SPP: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34BB")

    var onState: ((connected: Boolean, info: String) -> Unit)? = null
    var onData: ((text: String) -> Unit)? = null

    @Volatile var connected = false
        private set
    @Volatile var deviceName = ""
        private set

    private var socket: BluetoothSocket? = null
    private var worker: Thread? = null
    private val ui = Handler(Looper.getMainLooper())

    @SuppressLint("MissingPermission")
    fun paired(adapter: BluetoothAdapter): List<BluetoothDevice> =
        try {
            adapter.bondedDevices.toList()
        } catch (e: SecurityException) {
            emptyList()
        }

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        disconnect(silent = true)
        deviceName = try {
            device.name ?: device.address
        } catch (e: SecurityException) {
            device.address
        }
        ui.post { onState?.invoke(false, "Baglaniyor: $deviceName...") }
        worker = Thread {
            @Suppress("DEPRECATION")
            val adapter = BluetoothAdapter.getDefaultAdapter()
            try {
                adapter?.cancelDiscovery()
            } catch (_: SecurityException) {
            }
            val errors = mutableListOf<String>()
            // 1) Standart guvenli SPP soketi
            var s = tryConnect(errors) { device.createRfcommSocketToServiceRecord(SPP) }
            // 2) Guvensiz SPP (HC-05/HC-06 cogu telefonda bunu ister)
            if (s == null) {
                s = tryConnect(errors) { device.createInsecureRfcommSocketToServiceRecord(SPP) }
            }
            // 3) Kanal-1 yansima yontemi (klasik HC-05 "read failed" cozumu)
            if (s == null) {
                s = tryConnect(errors) {
                    @Suppress("UNCHECKED_CAST")
                    val m = device.javaClass.getMethod(
                        "createRfcommSocket",
                        Int::class.javaPrimitiveType
                    )
                    m.invoke(device, 1) as BluetoothSocket
                }
            }
            if (s == null) {
                connected = false
                ui.post { onState?.invoke(false, "Baglanamadi: ${errors.lastOrNull() ?: "bilinmiyor"}") }
                return@Thread
            }
            socket = s
            connected = true
            ui.post { onState?.invoke(true, "Bagli: $deviceName") }
            readLoop(s)
        }.also { it.start() }
    }

    private fun tryConnect(errors: MutableList<String>, open: () -> BluetoothSocket): BluetoothSocket? {
        var s: BluetoothSocket? = null
        return try {
            s = open()
            s!!.connect()
            s
        } catch (e: Exception) {
            errors.add(e.message ?: e.javaClass.simpleName)
            try {
                s?.close()
            } catch (_: IOException) {
            }
            null
        }
    }

    private fun readLoop(s: BluetoothSocket) {
        val buf = ByteArray(1024)
        try {
            val inp = s.inputStream
            while (connected) {
                val n = inp.read(buf)
                if (n <= 0) break
                // Arduino/HC tarafi "\r\n" gonderir; ekranda garip gorunmemesi icin normalize et
                val text = String(buf, 0, n, Charsets.UTF_8)
                    .replace("\r\n", "\n")
                    .replace("\r", "\n")
                ui.post { onData?.invoke(text) }
            }
        } catch (_: IOException) {
            // soket kapandi, donguden cik
        }
        if (connected) {
            connected = false
            ui.post { onState?.invoke(false, "Baglanti koptu") }
        }
        closeQuietly()
    }

    fun send(text: String): Boolean {
        val s = socket ?: return false
        if (!connected) return false
        return try {
            s.outputStream.write(text.toByteArray(Charsets.UTF_8))
            true
        } catch (_: IOException) {
            false
        }
    }

    fun disconnect(silent: Boolean = false) {
        connected = false
        try {
            worker?.interrupt()
        } catch (_: Exception) {
        }
        worker = null
        closeQuietly()
        if (!silent) ui.post { onState?.invoke(false, "Bagli degil") }
    }

    private fun closeQuietly() {
        try {
            socket?.close()
        } catch (_: IOException) {
        }
        socket = null
    }
}
