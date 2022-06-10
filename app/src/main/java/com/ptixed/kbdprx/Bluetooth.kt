package com.ptixed.kbdprx

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.util.Log
import java.util.*

@SuppressLint("MissingPermission")
class Bluetooth: BluetoothGattServerCallback
{
    private var activity: MainActivity
    private var bleman: BluetoothManager
    private var gatt: BluetoothGattServer
    private var hidReport: BluetoothGattCharacteristic

    private var device: BluetoothDevice? = null

    private var serviceState = 0

    private var connectCallback = object: AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int)
        {
            super.onStartFailure(errorCode)
            Log.i(this::class.simpleName, "Advertising failed with error $errorCode")
        }

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?)
        {
            super.onStartSuccess(settingsInEffect)
            Log.i(this::class.simpleName, "Advertising started")
        }
    }
    private var disconnectCallback = object: AdvertiseCallback() { }

    constructor(activity: MainActivity, bleman: BluetoothManager, map: ByteArray)
    {
        // TODO: add documentation (...link?)
        this.activity = activity
        this.bleman = bleman
        gatt = bleman.openGattServer(activity, this)

        var hid = BluetoothGattService(UUID.fromString("00001812-0000-1000-8000-00805F9B34FB"), BluetoothGattService.SERVICE_TYPE_PRIMARY)

        hidReport = BluetoothGattCharacteristic(
            UUID.fromString("00002A4D-0000-1000-8000-00805F9B34FB"),
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED)
        hidReport.value = byteArrayOf(0, 0, 0, 0, 0, 0, 0 ,0)

        var hidReportDescriptor = BluetoothGattDescriptor(UUID.fromString("00002902-0000-1000-8000-00805F9B34FB"),
            BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED or BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED)
        hidReportDescriptor.value = byteArrayOf(0, 0)
        hidReport?.addDescriptor(hidReportDescriptor)

        var hidReportReference = BluetoothGattDescriptor(UUID.fromString("00002908-0000-1000-8000-00805F9B34FB"), BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED)
        hidReportReference.value = byteArrayOf(0, 1)
        hidReport?.addDescriptor(hidReportReference)

        hid.addCharacteristic(hidReport)

        var hidMap = BluetoothGattCharacteristic(
            UUID.fromString("00002A4B-0000-1000-8000-00805F9B34FB"),
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED)
        hidMap.value = map

        var hidMapReference = BluetoothGattDescriptor(UUID.fromString("00002907-0000-1000-8000-00805F9B34FB"), BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED)
        hidMapReference.value = byteArrayOf(0, 0)
        hidMap.addDescriptor(hidMapReference)

        hid.addCharacteristic(hidMap)

        var hidControl = BluetoothGattCharacteristic(
            UUID.fromString("00002A4C-0000-1000-8000-00805F9B34FB"),
            BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED)
        hidControl.value = byteArrayOf(0)

        hid.addCharacteristic(hidControl)

        var hidInformation = BluetoothGattCharacteristic(
            UUID.fromString("00002A4A-0000-1000-8000-00805F9B34FB"),
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED)
        hidInformation.value = byteArrayOf(1, 0x11, 0, 2)

        hid.addCharacteristic(hidInformation)

        gatt.addService(hid)
    }

    fun destroy()
    {
        activity.setStatus("Off")
        bleman.adapter.bluetoothLeAdvertiser.stopAdvertising(disconnectCallback)
        gatt.close()
    }

    override fun onServiceAdded(status: Int, service: BluetoothGattService?)
    {
        //super.onServiceAdded(status, service)
        when (serviceState)
        {
            0 -> {
                serviceState = 1

                var battery = BluetoothGattService(UUID.fromString("0000180F-0000-1000-8000-00805F9B34FB"), BluetoothGattService.SERVICE_TYPE_SECONDARY)

                var batteryLevel = BluetoothGattCharacteristic(
                    UUID.fromString("00002A19-0000-1000-8000-00805F9B34FB"),
                    BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                    BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED)
                batteryLevel.value = byteArrayOf(50) // TODO: could be phone's battery level

                battery.addCharacteristic(batteryLevel)
                gatt.addService(battery)
            }
            1 -> {
                serviceState = 2

                var information = BluetoothGattService(UUID.fromString("0000180A-0000-1000-8000-00805F9B34FB"), BluetoothGattService.SERVICE_TYPE_SECONDARY)
                gatt.addService(information)
            }
            2 -> {
                serviceState = 3

                var settings = AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                    .setConnectable(true)
                    .setTimeout(0)
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                    .build()

                var data = AdvertiseData.Builder()
                    .setIncludeDeviceName(true)
                    //.addServiceUuid(ParcelUuid(hid.uuid)) // causes ADVERTISE_FAILED_DATA_TOO_LARGE
                    .build()

                activity.setStatus("Advertising")
                bleman.adapter.bluetoothLeAdvertiser.startAdvertising(settings, data, connectCallback)
            }
        }
    }

    override fun onCharacteristicReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic?)
    {
        Log.d(this::class.simpleName, characteristic!!.uuid.toString())
        gatt.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic!!.value)
    }

    override fun onDescriptorReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, descriptor: BluetoothGattDescriptor?)
    {
        Log.d(this::class.simpleName, descriptor!!.uuid.toString())
        gatt.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, descriptor!!.value)
    }

    override fun onCharacteristicWriteRequest(device: BluetoothDevice?,
                                              requestId: Int,
                                              characteristic: BluetoothGattCharacteristic?,
                                              preparedWrite: Boolean,
                                              responseNeeded: Boolean,
                                              offset: Int,
                                              value: ByteArray?)
    {
        gatt.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, byteArrayOf());
    }

    override fun onDescriptorWriteRequest(device: BluetoothDevice?,
                                          requestId: Int,
                                          descriptor: BluetoothGattDescriptor?,
                                          preparedWrite: Boolean,
                                          responseNeeded: Boolean,
                                          offset: Int,
                                          value: ByteArray?)
    {
        gatt.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, byteArrayOf());
    }

    override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int)
    {
        super.onConnectionStateChange(device, status, newState)
        Log.i(this::class.simpleName, "${this::onConnectionStateChange.name} $status $newState")

        bleman.adapter.bluetoothLeAdvertiser.stopAdvertising(disconnectCallback)
        activity.setStatus("Paired")

        if (newState == BluetoothProfile.STATE_CONNECTED)
            this.device = device
        else if (this.device == device && newState == BluetoothProfile.STATE_DISCONNECTED)
            this.device = null
    }

    fun report(report: ByteArray)
    {
        if (this.device == null)
            return
        Log.d(this::class.simpleName, report.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) })

        hidReport.value = report
        gatt.notifyCharacteristicChanged(device, hidReport, false)
    }
}
