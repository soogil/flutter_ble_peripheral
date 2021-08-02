## 0.5.0+1
Changes of 0.5.0 weren't visible on pub.dev

## 0.5.0
Added isSupported function to check if BLE advertising is supported by the device.

## 0.4.2
Fixed typo causing deviceName not to broadcast on iOS

## 0.4.1
Fixed bug on iOS which led to crash
Added local name to advertising in iOS
Updated Android dependencies

## 0.4.0
Added new options to AdvertiseData
Removed embedding V1 for Android

## 0.3.0
Upgraded to null-safety
Updated dependencies
Changed to pedantic

Bug fixes
* Fixed null-pointer when bluetooth adapter isn't found

## 0.2.0
Add support for MacOS

## 0.1.0
Fixed several parts for Android:
* Advertising local name
* Advertising Manufacturer Data
* Advertising Service Data

## 0.0.4
Fixed iOS advertising not working

## 0.0.3
Fixed callback on Android

## 0.0.2
Fixed flutter v2 embedding

## 0.0.1
Initial version of the library. This version includes:
* broadcasting a custom UUID
