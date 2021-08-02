/*
 * Copyright (c) 2020. Julian Steenbakker.
 * All rights reserved. Use of this source code is governed by a
 * BSD-style license that can be found in the LICENSE file.
 */

package dev.steenbakker.flutter_ble_peripheral

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.util.*
import kotlin.random.Random


class FlutterBlePeripheralPlugin: FlutterPlugin, MethodChannel.MethodCallHandler, EventChannel.StreamHandler {

  private val CHARACTERISTIC_USER_DESCRIPTION_UUID = UUID
          .fromString("00002901-0000-1000-8000-00805f9b34fb")
  private val CLIENT_CHARACTERISTIC_CONFIGURATION_UUID = UUID
          .fromString("00002902-0000-1000-8000-00805f9b34fb")
  private val testServiceUuid: UUID = UUID
          .fromString("bf27730d-860a-4e09-889c-2d8b6a9e0fe7")
  private val testSendUuid = UUID
          .fromString("00002A19-0000-1000-8000-00805f9b34fb")
  private var methodChannel: MethodChannel? = null
  private var eventChannel: EventChannel? = null
  private var peripheral: Peripheral = Peripheral()
  private var context: Context? = null
  private var kpnpGattService: BluetoothGattService? = null
  private var kpnpGattCharacteristic: BluetoothGattCharacteristic? = null

  private var eventSink: EventChannel.EventSink? = null
  private var advertiseCallback: (Boolean) -> Unit = { isAdvertising ->
    eventSink?.success(isAdvertising)
  }


  /** Plugin registration embedding v2 */
  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    initBluetoothGatt()
    methodChannel = MethodChannel(flutterPluginBinding.binaryMessenger, "dev.steenbakker.flutter_ble_peripheral/ble_state")
    eventChannel = EventChannel(flutterPluginBinding.binaryMessenger, "dev.steenbakker.flutter_ble_peripheral/ble_event")
    methodChannel!!.setMethodCallHandler(this)
    eventChannel!!.setStreamHandler(this)
    peripheral.init()
    context = flutterPluginBinding.applicationContext
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    methodChannel!!.setMethodCallHandler(null)
    methodChannel = null
    eventChannel!!.setStreamHandler(null)
    eventChannel = null
  }
  
  // TODO: Add permission check
  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: MethodChannel.Result) {
    when (call.method) {
      "start" -> startPeripheral(call, result)
      "stop" -> stopPeripheral(result)
      "isAdvertising" -> result.success(peripheral.isAdvertising())
      "isSupported" -> isSupported(result)
      else -> result.notImplemented()
    }
  }

  private fun initBluetoothGatt() {
    kpnpGattCharacteristic = BluetoothGattCharacteristic(testSendUuid,
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ)

    kpnpGattCharacteristic!!.addDescriptor(
            getClientCharacteristicConfigurationDescriptor())

    kpnpGattCharacteristic!!.addDescriptor(
            getCharacteristicUserDescriptionDescriptor("BATTERY_LEVEL_DESCRIPTION"))

    kpnpGattService = BluetoothGattService(testServiceUuid,
            BluetoothGattService.SERVICE_TYPE_PRIMARY)
    kpnpGattService!!.addCharacteristic(kpnpGattCharacteristic)
  }

  private fun getClientCharacteristicConfigurationDescriptor(): BluetoothGattDescriptor {
    val descriptor = BluetoothGattDescriptor(
            CLIENT_CHARACTERISTIC_CONFIGURATION_UUID,
            BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE)
    descriptor.value = byteArrayOf(0, 0)
    return descriptor
  }

  private fun getCharacteristicUserDescriptionDescriptor(defaultValue: String): BluetoothGattDescriptor {
    val descriptor = BluetoothGattDescriptor(
            CHARACTERISTIC_USER_DESCRIPTION_UUID,
            BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE)
    try {
      descriptor.value = defaultValue.toByteArray(charset("UTF-8"))
    } finally {
      return descriptor
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun startPeripheral(call: MethodCall, result: MethodChannel.Result) {
//    kpnpGattCharacteristic!!.setValue(Random(100).nextInt(),
//            BluetoothGattCharacteristic.FORMAT_UINT8, /* offset */ 0)

    if (call.arguments !is Map<*, *>) {
      throw IllegalArgumentException("Arguments are not a map! " + call.arguments)
    }

    val arguments = call.arguments as Map<String, Any>
    val advertiseData = Data()
    (arguments["uuid"] as String?)?.let { advertiseData.uuid = it }
    (arguments["manufacturerId"] as Int?)?.let { advertiseData.manufacturerId = it }
    (arguments["manufacturerData"] as List<Int>?)?.let { advertiseData.manufacturerData = it }
    (arguments["serviceDataUuid"] as String?)?.let { advertiseData.serviceDataUuid = it }
    (arguments["serviceData"] as List<Int>?)?.let { advertiseData.serviceData = it }
    (arguments["includeDeviceName"] as Boolean?)?.let { advertiseData.includeDeviceName = it }
    (arguments["transmissionPowerIncluded"] as Boolean?)?.let { advertiseData.includeTxPowerLevel = it }
    (arguments["advertiseMode"] as Int?)?.let { advertiseData.advertiseMode = it }
    (arguments["connectable"] as Boolean?)?.let { advertiseData.connectable = it }
    (arguments["timeout"] as Int?)?.let { advertiseData.timeout = it }
    (arguments["txPowerLevel"] as Int?)?.let { advertiseData.txPowerLevel = it }
    
    val advertiseSettings: AdvertiseSettings = AdvertiseSettings.Builder()
            .setAdvertiseMode(advertiseData.advertiseMode)
            .setConnectable(advertiseData.connectable)
            .setTimeout(advertiseData.timeout)
            .setTxPowerLevel(advertiseData.txPowerLevel)
            .build()

    peripheral.start(advertiseData, advertiseSettings, advertiseCallback)
    result.success(null)
  }

  private fun stopPeripheral(result: MethodChannel.Result) {
    peripheral.stop()
    result.success(null)
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
}

