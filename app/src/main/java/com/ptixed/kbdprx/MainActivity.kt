package com.ptixed.kbdprx

import android.Manifest
import android.bluetooth.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlin.math.ceil

// adb tcpip 5555
// adb usb
class MainActivity : AppCompatActivity()
{
    private var ble: Bluetooth? = null
    private var kbd: Keyboard? = null

    private var counter = 0
    private lateinit var statusLabel: TextView

    private var launcher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (!isGranted)
                Toast.makeText(this, "Could not enable bluetooth", Toast.LENGTH_SHORT).show()
        }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.startButton).setOnClickListener(this::start)
        statusLabel = findViewById(R.id.statusLabel)
    }

    private fun start(view: View)
    {
        try
        {
            kbd?.destroy()
            kbd = Keyboard.open(this::onReport)
        }
        catch (ex: Exception)
        {
            Toast.makeText(this, ex.message, Toast.LENGTH_SHORT).show()
            return
        }

        var bleman = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        var adapter = bleman.adapter

        if (adapter == null || !adapter.isEnabled)
        {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED)
                launcher.launch(Manifest.permission.BLUETOOTH)
            else
                startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            return
        }

        if (!adapter.isMultipleAdvertisementSupported)
        {
            Toast.makeText(this, "Advertising not supported", Toast.LENGTH_SHORT).show()
            return
        }

        ble?.destroy()
        ble = Bluetooth(this, bleman, kbd!!.map)
    }

    private fun onReport(report: ByteArray)
    {
        ble?.report(report)
        ++counter
        runOnUiThread {
            statusLabel.text = ceil(counter / 2.0).toInt().toString()
        }
    }
}