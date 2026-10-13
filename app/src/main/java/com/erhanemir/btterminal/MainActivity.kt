package com.erhanemir.btterminal

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.erhanemir.btterminal.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private var adapter: BluetoothAdapter? = null
    val logBuffer = StringBuilder()

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    private val enableBtLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (adapter?.isEnabled == true) showDeviceDialog()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        val mgr = getSystemService(BluetoothManager::class.java)
        adapter = mgr?.adapter
        if (adapter == null) {
            Toast.makeText(this, "Bu cihazda Bluetooth yok", Toast.LENGTH_LONG).show()
            b.connectBtn.isEnabled = false
        }

        BtManager.onState = { connected, info ->
            b.statusText.text = info
            b.statusDot.setBackgroundResource(if (connected) R.drawable.dot_on else R.drawable.dot_off)
        }
        BtManager.onData = { text ->
            logBuffer.append(text)
            if (logBuffer.length > 30000) logBuffer.delete(0, logBuffer.length - 30000)
            (supportFragmentManager.findFragmentById(R.id.fragmentHost) as? TerminalFragment)
                ?.appendText(text)
        }

        b.connectBtn.setOnClickListener { ensurePermsThen { ensureBtOnThen { showDeviceDialog() } } }
        b.disconnectBtn.setOnClickListener { BtManager.disconnect() }
        b.updateBtn.setOnClickListener {
            Toast.makeText(this, "Guncelleme denetleniyor...", Toast.LENGTH_SHORT).show()
            UpdateManager.check(this, silent = false) { upd, err ->
                if (upd != null) askUpdate(upd)
                else if (err != null) Toast.makeText(this, "Kontrol edilemedi: $err", Toast.LENGTH_LONG).show()
                else Toast.makeText(this, "Zaten guncelsin (${UpdateManager.currentVersion(this)})", Toast.LENGTH_SHORT).show()
            }
        }
        // Acilista sessiz kontrol
        UpdateManager.check(this, silent = true) { upd, _ ->
            if (upd != null) askUpdate(upd)
        }

        b.bottomNav.setOnItemSelectedListener { item ->
            val f: Fragment = when (item.itemId) {
                R.id.nav_keypad -> KeypadFragment()
                else -> TerminalFragment()
            }
            supportFragmentManager.beginTransaction().replace(R.id.fragmentHost, f).commit()
            true
        }
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().replace(R.id.fragmentHost, TerminalFragment()).commit()
        }
        ensurePermsThen { }
    }

    private fun neededPerms(): Array<String> =
        if (Build.VERSION.SDK_INT >= 31) {
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    private fun ensurePermsThen(action: () -> Unit) {
        val missing = neededPerms().filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) action()
        else permLauncher.launch(missing.toTypedArray())
    }

    private fun ensureBtOnThen(action: () -> Unit) {
        if (adapter?.isEnabled == true) action()
        else enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
    }

    private fun askUpdate(upd: UpdateManager.Update) {
        AlertDialog.Builder(this)
            .setTitle("Yeni surum: ${upd.tag}")
            .setMessage("Indirilip kurulsun mu?")
            .setPositiveButton("Guncelle") { _, _ -> UpdateManager.download(this, upd.url) }
            .setNegativeButton("Sonra", null)
            .show()
    }

    private fun showDeviceDialog() {        val ad = adapter ?: return
        val devices = BtManager.paired(ad)
        if (devices.isEmpty()) {
            Toast.makeText(
                this,
                "Eslesmis cihaz yok. Once telefonun Bluetooth ayarlarindan HC-05/HC-06 ile esles.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val names = devices.map {
            try {
                "${it.name ?: "?"} (${it.address})"
            } catch (e: SecurityException) {
                it.address
            }
        }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Cihaz sec")
            .setItems(names) { _, which -> BtManager.connect(devices[which]) }
            .show()
    }

    override fun onDestroy() {
        BtManager.disconnect(silent = true)
        super.onDestroy()
    }
}
