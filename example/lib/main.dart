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

class _FlutterBlePeripheralExampleState extends State<FlutterBlePeripheralExample> {
  final FlutterBlePeripheral blePeripheral = FlutterBlePeripheral();
  final AdvertiseData _data = AdvertiseData();
  double sliderValue = 0;

  @override
  void initState() {
    super.initState();
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
                  Slider(
                    value: sliderValue,
                    onChanged: (value) {
                      setState(() {
                        sliderValue = value.roundToDouble();
                      });
                    },
                    min: 0,
                    max: 255,
                  ),
                  Text(
                    '강도 : $sliderValue',
                    style: const TextStyle(
                      fontSize: 20,
                      color: Colors.black,
                    ),
                  ),
                  const SizedBox(height: 50,),
                  Row(
                    children: List.generate(3, (index) => _targetButton(index))
                  ),
                  // _doubleTargetButton()
                ]
            )
        ),
      ),
    );
  }

  Widget _doubleTargetButton() {
    return OutlinedButton(
        onPressed: () async {
          // if (await blePeripheral.isAdvertising()) {
          blePeripheral.sendData(strength: sliderValue,);
          // }
        },
        child: Text(
          'sendData',
          style: Theme
              .of(context)
              .primaryTextTheme
              .button!
              .copyWith(color: Colors.blue),
        )
    );
  }

  Widget _targetButton(int position) {
    return Expanded(
      child: Container(
        height: 60,
        margin: const EdgeInsets.only(left: 5, right: 5),
        child: ElevatedButton(
            onPressed: () {
              blePeripheral.sendData(strength: sliderValue, position: position + 1);
            },
            child: Text(
              (position + 1).toString(),
              style: const TextStyle(
                fontSize: 20,
                color: Colors.white,
              ),
            )
        ),
      ),
    );
  }
}
