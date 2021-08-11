
import 'package:flutter/material.dart';
import 'package:flutter_ble_peripheral_example/main.dart';

class LargeTargetMittPage extends StatefulWidget {
  const LargeTargetMittPage({Key? key}) : super(key: key);

  @override
  _LargeTargetMittPageState createState() =>
      _LargeTargetMittPageState();
}

class _LargeTargetMittPageState extends State<LargeTargetMittPage> {

  double sliderValue = 0;

  @override
  void initState() {
    blePeripheral.changeDeviceName(2).then(
            (value) => blePeripheral.startAdvertising(2));
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
                  _largeTargetButtons(),
                ]
            )
        ),
      ),
    );
  }

  Widget _largeTargetButtons() {
    return GridView.count(
      padding: const EdgeInsets.only(left: 10, right: 10),
      shrinkWrap: true,
      crossAxisCount: 3,
      crossAxisSpacing: 10,
      mainAxisSpacing: 10,
      children: List.generate(6, (index) => _largeTargetButton(index)),
      childAspectRatio: 1.1,
    );
  }

  Widget _largeTargetButton(int position) {
    return  ElevatedButton(
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
    );
  }
}
