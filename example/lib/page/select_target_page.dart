import 'package:flutter/material.dart';
import 'package:flutter_ble_peripheral_example/page/authentic_target_mitt_page.dart';
import 'package:flutter_ble_peripheral_example/page/double_target_mitt_page.dart';
import 'package:flutter_ble_peripheral_example/page/large_target_mitt_page.dart';

class SelectTargetPage extends StatelessWidget {
  const SelectTargetPage({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(),
      body: _buildBody(context),
    );
  }

  Widget _buildBody(BuildContext context) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          SizedBox(
            height: 50,
            child: ElevatedButton(
                onPressed: () {
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (context) => const DoubleTargetMittPage(),
                    ),
                  );
                },
                child: const Text(
                  '더블 타겟 미트 테스트',
                  style: TextStyle(
                    color: Colors.black,
                    fontSize: 23,
                    fontWeight: FontWeight.bold,
                  ),
                )
            ),
          ),
          const SizedBox(height: 50,),
          SizedBox(
            height: 50,
            child: ElevatedButton(
                onPressed: () {
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (context) => const AuthenticTargetMittPage(),
                    ),
                  );
                },
                child: const Text(
                  '어센틱 타겟 미트 테스트',
                  style: TextStyle(
                    color: Colors.black,
                    fontSize: 23,
                    fontWeight: FontWeight.bold,
                  ),
                )
            ),
          ),
          const SizedBox(height: 50,),
          SizedBox(
            height: 50,
            child: ElevatedButton(
                onPressed: () {
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (context) => const LargeTargetMittPage(),
                    ),
                  );
                },
                child: const Text(
                  '대형 타겟 미트 테스트',
                  style: TextStyle(
                    color: Colors.black,
                    fontSize: 23,
                    fontWeight: FontWeight.bold,
                  ),
                )
            ),
          ),
        ],
      ),
    );
  }
}
