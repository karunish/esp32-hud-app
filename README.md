# ESP32 HUD Android App

A real-time mirrored heads-up display (HUD) for vehicles, built with Android and ESP32. This app uses GPS data from your phone and sends live speed updates over Bluetooth to an ESP32 microcontroller, which displays it on a physical screen — perfect for DIY automotive projects.
> ⚙️ This app is designed to work hand-in-hand with the ESP32 firmware I’ve developed. Together, they form a complete HUD system: Android handles GPS and Bluetooth, while the ESP32 receives and displays the speed data.

---

## Features

- Real-time GPS speed tracking
- Bluetooth connection to ESP32 (classic SPP)
- Foreground service for continuous updates
- Battery optimization prompts for uninterrupted operation
- Live status and speed display on phone UI
- ESP32 indicator when connected

---

## Requirements

- Android 12 or higher (tested on Android 15, One UI 7)
- ESP32 with [BluetoothSerial](https://github.com/espressif/arduino-esp32/tree/master/libraries/BluetoothSerial) enabled
- Paired device named `ESP32_HUD`
- Permissions:
  - `ACCESS_FINE_LOCATION`
  - `ACCESS_BACKGROUND_LOCATION`
  - `BLUETOOTH_CONNECT`
  - `BLUETOOTH_SCAN`
  - `FOREGROUND_SERVICE_LOCATION`
  - `FOREGROUND_SERVICE_CONNECTED_DEVICE`

---

## Setup

1. **Pair your ESP32** with your Android device via Bluetooth settings beforehand.
2. **Flash your ESP32** with this sketch or a sketch that uses:
   ```cpp
   SerialBT.begin("ESP32_HUD");
   ```
   **Make sure that your ESP32 display is laterally inverted to form a correct image when displayed on the windshield** 
