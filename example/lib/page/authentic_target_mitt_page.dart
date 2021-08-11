
import 'package:flutter/material.dart';
import 'package:flutter_ble_peripheral_example/main.dart';

class AuthenticTargetMittPage extends StatefulWidget {
  const AuthenticTargetMittPage({Key? key}) : super(key: key);

  @override
  _AuthenticTargetMittPageState createState() =>
      _AuthenticTargetMittPageState();
}

class _AuthenticTargetMittPageState extends State<AuthenticTargetMittPage> {

  double sliderValue = 0;

  @override
  void initState() {
    blePeripheral.changeDeviceName(1).then(
            (value) => blePeripheral.startAdvertising(1));
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
                  _authenticTargetButtons(),
                  // _authenticTargetButton(),
                  // _doubleTargetButton()
                ]
            )
        ),
      ),
    );
  }

  Widget _authenticTargetButtons() {
    return Row(
        children: List.generate(3, (index) => _authenticTargetButton(index))
    );
  }

  Widget _authenticTargetButton(int position,) {
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
