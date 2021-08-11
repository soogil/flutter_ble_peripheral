
import 'package:flutter/material.dart';
import 'package:flutter_ble_peripheral/flutter_ble_peripheral.dart';
import 'package:flutter_ble_peripheral_example/page/select_target_page.dart';

FlutterBlePeripheral blePeripheral = FlutterBlePeripheral();

void main() => runApp(
    const MaterialApp(
      home: SelectTargetPage(),
    ));
