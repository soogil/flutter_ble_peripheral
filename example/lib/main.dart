/*
 * Copyright (c) 2020. Julian Steenbakker.
 * All rights reserved. Use of this source code is governed by a
 * BSD-style license that can be found in the LICENSE file.
 */

import 'package:flutter/material.dart';
import 'package:flutter_ble_peripheral/flutter_ble_peripheral.dart';

void main() => runApp(const FlutterBlePeripheralExample());

class FlutterBlePeripheralExample extends StatefulWidget {
  const FlutterBlePeripheralExample({Key? key}) : super(key: key);

  @override
  _FlutterBlePeripheralExampleState createState() =>
      _FlutterBlePeripheralExampleState();
}

class _FlutterBlePeripheralExampleState
    extends State<FlutterBlePeripheralExample> {
  final FlutterBlePeripheral blePeripheral = FlutterBlePeripheral();
  final AdvertiseData _data = AdvertiseData();
  bool _isBroadcasting = false;
  bool? _isSupported;

  @override
  void initState() {
    super.initState();
    // setState(() {
    //   _data.includeDeviceName = false;
    //   _data.uuid = 'bf27730d-860a-4e09-889c-2d8b6a9e0fe7';
    //   _data.manufacturerId = 1234;
    //   _data.timeout = 1000;
    //   _data.manufacturerData = [1, 2, 3, 4, 5, 6];
    //   _data.txPowerLevel = AdvertisePower.ADVERTISE_TX_POWER_ULTRA_LOW;
    //   _data.advertiseMode = AdvertiseMode.ADVERTISE_MODE_LOW_LATENCY;
    // });
    // initPlatformState();
  }

  Future<void> initPlatformState() async {
    final isSupported = await blePeripheral.isSupported();
    setState(() {
      _isSupported = isSupported;
    });
  }

  void _toggleAdvertise() async {
    // if (await blePeripheral.isAdvertising()) {
    //   blePeripheral.sendData();
    //   await blePeripheral.stop();
    //   setState(() {
    //     _isBroadcasting = false;
    //   });
    // } else {
    //   await blePeripheral.start(_data);
    //   setState(() {
    //     _isBroadcasting = true;
    //   });
    // }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Flutter BLE Peripheral'),
        ),
        body: Center(
            child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                crossAxisAlignment: CrossAxisAlignment.center,
                children: <Widget>[
                  StreamBuilder<String>(
                    initialData: 'Devices Connected 0',
                    stream: blePeripheral.getConnectedDeviceCountToString,
                      builder: (context, snapShotData) {
                        if (!snapShotData.hasData) {
                          return Container();
                        }

                        return Text(snapShotData.data!);
                      }
                  ),
                  OutlinedButton(
                      onPressed: () async {
                        // if (await blePeripheral.isAdvertising()) {
                          blePeripheral.sendData();
                        // }
                      },
                      child: Text(
                        'sendData',
                        style: Theme.of(context)
                            .primaryTextTheme
                            .button!
                            .copyWith(color: Colors.blue),
                      )),
            ])),
      ),
    );
  }
}
