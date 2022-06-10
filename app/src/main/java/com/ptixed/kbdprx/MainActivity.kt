package com.ptixed.kbdprx

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
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
    private lateinit var counterLabel: TextView

    private var launcher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (!isGranted)
            {
                Toast.makeText(this, "Could not enable bluetooth", Toast.LENGTH_SHORT).show()
                statusLabel.text = "Off"
            }
            else
                start()
        }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<ImageButton>(R.id.startButton).setOnClickListener {
            start()
        }
        statusLabel = findViewById(R.id.statusLabel)
        counterLabel = findViewById(R.id.counterLabel)
    }

    private fun start()
    {
        statusLabel.text = "Initializing"

        var bleman = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        var adapter = bleman.adapter

        if (adapter == null || !adapter.isEnabled)
        {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED)
                launcher.launch(Manifest.permission.BLUETOOTH)
            else
            {
                var startActivityForResult = registerForActivityResult(StartActivityForResult()) { result: ActivityResult ->
                    if (result.resultCode == RESULT_OK)
                        start()
                }
                startActivityForResult.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
            }
            return
        }

        if (!adapter.isMultipleAdvertisementSupported)
        {
            statusLabel.text = "Off"
            Toast.makeText(this, "Advertising not supported", Toast.LENGTH_SHORT).show()
            return
        }

        try
        {
            kbd?.destroy()
            kbd = Keyboard.open(this::onReport)
        }
        catch (ex: Exception)
        {
            statusLabel.text = "Off"
            Toast.makeText(this, ex.message, Toast.LENGTH_SHORT).show()
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
            counterLabel.text = ceil(counter / 2.0).toInt().toString()
        }
    }

    public fun setStatus(status: String)
    {
        runOnUiThread {
            statusLabel.text = status
        }
    }
}