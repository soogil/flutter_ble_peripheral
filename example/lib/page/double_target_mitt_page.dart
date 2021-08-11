import 'package:flutter/material.dart';
import 'package:flutter_ble_peripheral_example/main.dart';

class DoubleTargetMittPage extends StatefulWidget {
  const DoubleTargetMittPage({Key? key}) : super(key: key);

  @override
  _DoubleTargetMittPageState createState() =>
      _DoubleTargetMittPageState();
}

class _DoubleTargetMittPageState extends State<DoubleTargetMittPage> {
  double sliderValue = 0;

  @override
  void initState() {
    blePeripheral.changeDeviceName(0).then(
            (value) => blePeripheral.startAdvertising(0));
    super.initState();
  }

  @override
  void dispose() {
    blePeripheral.stopAdvertising();
    super.dispose();
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
                  _doubleTargetButton(),
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
}