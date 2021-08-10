/*
 * Copyright (c) 2020. Julian Steenbakker.
 * All rights reserved. Use of this source code is governed by a
 * BSD-style license that can be found in the LICENSE file.
 */

package dev.steenbakker.flutter_ble_peripheral

import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import org.json.JSONObject
import java.util.*
import kotlin.collections.HashSet


class FlutterBlePeripheralPlugin: ActivityAware, FlutterPlugin, MethodChannel.MethodCallHandler, EventChannel.StreamHandler {

  private val characteristicUserDescription = UUID
          .fromString("00002901-0000-1000-8000-00805f9b34fb")
  private val characteristicConfigure = UUID
          .fromString("00002902-0000-1000-8000-00805f9b34fb")
  private val testServiceUuid: UUID = UUID
          .fromString("bf27730d-860a-4e09-889c-2d8b6a9e0fe7")
  private val testSendUuid = UUID
          .fromString("00002A19-0000-1000-8000-00805f9b34fb")
  private var methodChannel: MethodChannel? = null
  private var eventChannel: EventChannel? = null
//  private var peripheral: Peripheral = Peripheral()
  private var context: Context? = null

  private var kpnpGattService: BluetoothGattService? = null
  private var kpnpGattCharacteristic: BluetoothGattCharacteristic? = null
  private var bluetoothDevices: HashSet<BluetoothDevice> = HashSet()
  private var bluetoothManager: BluetoothManager? = null
  private var bluetoothAdapter: BluetoothAdapter? = null
  private var advertiser: BluetoothLeAdvertiser? = null

  private var advData: AdvertiseData? = null
  private var advScanResponse: AdvertiseData? = null
  private var advSettings: AdvertiseSettings? = null

  private var eventSink: EventChannel.EventSink? = null
  private val uiThreadHandler: Handler = Handler(Looper.getMainLooper())

  private val advCallback: AdvertiseCallback = object : AdvertiseCallback() {
    override fun onStartFailure(errorCode: Int) {
      super.onStartFailure(errorCode)
      Log.e(TAG, "Not broadcasting: $errorCode")
      val statusText: String
      when (errorCode) {
        ADVERTISE_FAILED_ALREADY_STARTED -> {
          statusText = "Failed already started"
          Log.w(TAG, "App was already advertising")
        }
        ADVERTISE_FAILED_DATA_TOO_LARGE -> statusText = "Failed data too large"
        ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> statusText = "Failed feature unsupported"
        ADVERTISE_FAILED_INTERNAL_ERROR -> statusText = "Failed internal error"
        ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> statusText = "Failed too many advertisers"
        else -> {
          statusText = "Unhandled error"
          Log.wtf(TAG, "Unhandled error: $errorCode")
        }
      }
//      mAdvStatus.setText(statusText)
    }

    override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
      super.onStartSuccess(settingsInEffect)
      println("onStartSuccess Broadcasting")
    }
  }

  /** Plugin registration embedding v2 */
  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    methodChannel = MethodChannel(flutterPluginBinding.binaryMessenger, "dev.steenbakker.flutter_ble_peripheral/ble_state")
    eventChannel = EventChannel(flutterPluginBinding.binaryMessenger, "dev.steenbakker.flutter_ble_peripheral/ble_event")
    methodChannel!!.setMethodCallHandler(this)
    eventChannel!!.setStreamHandler(this)
    context = flutterPluginBinding.applicationContext
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    methodChannel!!.setMethodCallHandler(null)
    methodChannel = null
    eventChannel!!.setStreamHandler(null)
    eventChannel = null
    advertiser!!.stopAdvertising(advCallback)
  }
  
  // TODO: Add permission check
  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: MethodChannel.Result) {
    when (call.method) {
      "sendData" -> sendData(call, result)
      "isSupported" -> isSupported(result)
      else -> result.notImplemented()
    }
  }

  private fun initBluetoothGatt() {
    bluetoothManager =  context!!.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    bluetoothAdapter = bluetoothManager!!.adapter
//    bluetoothAdapter!!.name = "Double Target Mitt"
    bluetoothAdapter!!.name = "Authentic Target Mitt"
//    bluetoothAdapter!!.name = "Large Target Mitt"

    kpnpGattCharacteristic = BluetoothGattCharacteristic(testSendUuid,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ)

    kpnpGattCharacteristic!!.addDescriptor(
            getClientCharacteristicConfigurationDescriptor())

    kpnpGattCharacteristic!!.addDescriptor(
            getCharacteristicUserDescriptionDescriptor("BATTERY_LEVEL_DESCRIPTION"))

    kpnpGattService = BluetoothGattService(testServiceUuid,
            BluetoothGattService.SERVICE_TYPE_PRIMARY)
    kpnpGattService!!.addCharacteristic(kpnpGattCharacteristic)

    gattServer = bluetoothManager!!.openGattServer(context, gattServerCallback)

    if (gattServer == null) {
      ensureBleFeaturesAvailable()
      return
    }
    // Add a service for a total of three services (Generic Attribute and Generic Access
    // are present by default).
    gattServer!!.addService(kpnpGattService)

    initAdvertiser()
  }

  private fun initAdvertiser() {
    val parcelUuid = ParcelUuid(testServiceUuid)

    advSettings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(true)
            .build()
    advData = AdvertiseData.Builder()
            .setIncludeTxPowerLevel(true)
            .addServiceData(parcelUuid, "1".toByteArray())
//            .addServiceUuid(parcelUuid)
//            .addServiceData(parcelUuid, null)
            .build()
    advScanResponse = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .build()

    if (bluetoothAdapter!!.isMultipleAdvertisementSupported) {
      advertiser = bluetoothAdapter!!.bluetoothLeAdvertiser
      advertiser!!.startAdvertising(advSettings, advData, advScanResponse, advCallback);
    }
  }

  private fun getClientCharacteristicConfigurationDescriptor(): BluetoothGattDescriptor {
    val descriptor = BluetoothGattDescriptor(
            characteristicConfigure,
            BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE)
    descriptor.value = byteArrayOf(0, 0)
    return descriptor
  }

  private fun getCharacteristicUserDescriptionDescriptor(defaultValue: String): BluetoothGattDescriptor {
    val descriptor = BluetoothGattDescriptor(
            characteristicUserDescription,
            BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE)
    try {
      descriptor.value = defaultValue.toByteArray(charset("UTF-8"))
    } finally {
      return descriptor
    }
  }

  private fun sendData(call: MethodCall, result: MethodChannel.Result) {
    kpnpGattCharacteristic!!.setValue(call.arguments.toString().toByteArray())
    val indicate = ((kpnpGattCharacteristic!!.properties
            and BluetoothGattCharacteristic.PROPERTY_INDICATE)
            == BluetoothGattCharacteristic.PROPERTY_INDICATE)
    for (device in bluetoothDevices) {
      // true for indication (acknowledge) and false for notification (unacknowledge).
      gattServer!!.notifyCharacteristicChanged(device, kpnpGattCharacteristic, indicate)
    }
  }

  override fun onListen(event: Any?, eventSink: EventChannel.EventSink) {
    this.eventSink = eventSink
  }

  override fun onCancel(event: Any?) {
    this.eventSink = null
  }

  private fun isSupported(result: MethodChannel.Result) {
    if (context != null) {
      val pm: PackageManager = context!!.packageManager
      result.success(pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
    } else {
      result.error("isSupported", "No context available", null)
    }
  }

  private var gattServer: BluetoothGattServer? = null
  private val gattServerCallback: BluetoothGattServerCallback = object : BluetoothGattServerCallback() {
    override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
      super.onConnectionStateChange(device, status, newState)
      if (status == BluetoothGatt.GATT_SUCCESS) {
        if (newState == BluetoothGatt.STATE_CONNECTED) {
          bluetoothDevices.add(device)
          updateConnectedDevicesStatus()
          println("Connected to device: ${device.address}")
        } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
          bluetoothDevices.remove(device)
          updateConnectedDevicesStatus()
          println("Disconnected from device")
        }
      } else {
        bluetoothDevices.remove(device)
        updateConnectedDevicesStatus()
        // There are too many gatt errors (some of them not even in the documentation) so we just
        // show the error to the user.
//        val errorMessage: String = getString(R.string.status_errorWhenConnecting).toString() + ": " + status
//        runOnUiThread(Runnable { Toast.makeText(this@Peripheral, errorMessage, Toast.LENGTH_LONG).show() })
        Log.e(TAG, "Error when connecting: $status")
        println("Error when connecting: $status")
      }
    }

    override fun onCharacteristicReadRequest(device: BluetoothDevice, requestId: Int, offset: Int,
                                             characteristic: BluetoothGattCharacteristic) {
      super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
      Log.d(TAG, "Device tried to read characteristic: " + characteristic.uuid)
      Log.d(TAG, "Value: " + Arrays.toString(characteristic.value))
      if (offset != 0) {
        gattServer!!.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, offset,  /* value (optional) */
                null)
        return
      }
      gattServer!!.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
              offset, characteristic.value)
    }

    override fun onNotificationSent(device: BluetoothDevice, status: Int) {
      println("onNotificationSent ${device.name} ${status}")
      super.onNotificationSent(device, status)
    }

    override fun onCharacteristicWriteRequest(device: BluetoothDevice, requestId: Int,
                                              characteristic: BluetoothGattCharacteristic, preparedWrite: Boolean, responseNeeded: Boolean,
                                              offset: Int, value: ByteArray) {
      super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite,
              responseNeeded, offset, value)
      println("Characteristic Write request: ${Arrays.toString(value)}")
//      Log.v(TAG, "Characteristic Write request: " + Arrays.toString(value))
//      val status: Int = mCurrentServiceFragment.writeCharacteristic(characteristic, offset, value)
      if (responseNeeded) {
        gattServer!!.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,  /* No need to respond with an offset */
                0,  /* No need to respond with a value */
                null)
      }
    }

    override fun onDescriptorReadRequest(device: BluetoothDevice, requestId: Int,
                                         offset: Int, descriptor: BluetoothGattDescriptor) {
      super.onDescriptorReadRequest(device, requestId, offset, descriptor)
      Log.d(TAG, "Device tried to read descriptor: " + descriptor.uuid)
      Log.d(TAG, "Value: " + Arrays.toString(descriptor.value))
      if (offset != 0) {
        gattServer!!.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, offset,  /* value (optional) */
                null)
        return
      }
      gattServer!!.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, descriptor.value)
    }

    override fun onDescriptorWriteRequest(device: BluetoothDevice, requestId: Int,
                                          descriptor: BluetoothGattDescriptor, preparedWrite: Boolean, responseNeeded: Boolean,
                                          offset: Int,
                                          value: ByteArray) {
      super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded,
              offset, value)
      Log.v(TAG, "Descriptor Write Request " + descriptor.uuid + " " + Arrays.toString(value))
      var status = BluetoothGatt.GATT_SUCCESS
      if (descriptor.uuid === characteristicConfigure) {
        val characteristic = descriptor.characteristic
        val supportsNotifications = characteristic.properties and
                BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0
        val supportsIndications = characteristic.properties and
                BluetoothGattCharacteristic.PROPERTY_INDICATE != 0
        if (!(supportsNotifications || supportsIndications)) {
          status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED
        } else if (value.size != 2) {
          status = BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH
        } else if (Arrays.equals(value, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
          status = BluetoothGatt.GATT_SUCCESS
//          mCurrentServiceFragment.notificationsDisabled(characteristic)
          descriptor.value = value
        } else if (supportsNotifications &&
                Arrays.equals(value, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
          status = BluetoothGatt.GATT_SUCCESS
//          mCurrentServiceFragment.notificationsEnabled(characteristic, false /* indicate */)
          descriptor.value = value
        } else if (supportsIndications &&
                Arrays.equals(value, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)) {
          status = BluetoothGatt.GATT_SUCCESS
//          mCurrentServiceFragment.notificationsEnabled(characteristic, true /* indicate */)
          descriptor.value = value
        } else {
          status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED
        }
      } else {
        status = BluetoothGatt.GATT_SUCCESS
        descriptor.value = value
      }
      if (responseNeeded) {
        gattServer!!.sendResponse(device, requestId, status,  /* No need to respond with offset */
                0,  /* No need to respond with a value */
                null)
      }
    }
  }

  private fun ensureBleFeaturesAvailable() {
//    if (bluetoothAdapter == null) {
////      Toast.makeText(this, R.string.bluetoothNotSupported, Toast.LENGTH_LONG).show()
//      Log.e(TAG, "Bluetooth not supported")
////      finish()
//    } else if (!bluetoothAdapter!!.isEnabled) {
//      val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
//      startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
//    }
  }

  private fun updateConnectedDevicesStatus() {
    val message: String = "Devices Connected" +
            " ${bluetoothManager?.getConnectedDevices(BluetoothGattServer.GATT)?.size}"

    uiThreadHandler.post { this.eventSink?.success(message) }
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    initBluetoothGatt()
  }

  override fun onDetachedFromActivityForConfigChanges() {
    TODO("Not yet implemented")
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    TODO("Not yet implemented")
  }

  override fun onDetachedFromActivity() {
    TODO("Not yet implemented")
  }
}

