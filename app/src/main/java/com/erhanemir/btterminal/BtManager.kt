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
            try {
                val adapter = BluetoothAdapter.getDefaultAdapter()
                try {
                    adapter?.cancelDiscovery()
                } catch (_: SecurityException) {
                }
                val s = device.createRfcommSocketToServiceRecord(SPP)
                socket = s
                s.connect()
                connected = true
                ui.post { onState?.invoke(true, "Bagli: $deviceName") }
                readLoop(s)
            } catch (e: IOException) {
                closeQuietly()
                connected = false
                ui.post { onState?.invoke(false, "Baglanti hatasi: ${e.message}") }
            } catch (e: SecurityException) {
                connected = false
                ui.post { onState?.invoke(false, "Izin hatasi: ${e.message}") }
            }
        }.also { it.start() }
    }

    private fun readLoop(s: BluetoothSocket) {
        val buf = ByteArray(1024)
        try {
            val inp = s.inputStream
            while (connected) {
                val n = inp.read(buf)
                if (n <= 0) break
                val text = String(buf, 0, n, Charsets.UTF_8)
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
