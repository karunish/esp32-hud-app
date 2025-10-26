<p align="center">
  <img src="https://github.com/user-attachments/assets/dcc5510c-8d9b-4a4a-b452-34c25b70dda1" alt="App Logo" width="120" />
</p>

# ESP32 HUD Android App

A real-time mirrored heads-up display (HUD) for vehicles, built with Android and ESP32. This app uses GPS data from your phone and sends live speed updates over Bluetooth to an ESP32 microcontroller, which displays it on a physical screen, perfect for DIY automotive projects.
> ⚙️ This app is designed to work hand-in-hand with the [ESP32 code I’ve developed](https://github.com/karunish/esp32-hud). Together, they form a complete HUD system: Android handles GPS and Bluetooth, while the ESP32 receives and displays the speed data.

---

## Features

- **Real-time GPS speed tracking** with interpolation for smooth display on ESP32.
- **Speed limit forwarding**: sends current road speed limit to ESP32 (`LIMIT:XX`).
- **Bluetooth connection** to ESP32 (classic SPP).
- **Foreground service** for continuous updates, even with the screen off.
- **Battery optimization prompts** to prevent Android from killing the service.
- **Notification channel handling**: defensive setup to avoid OEM quirks (Samsung, etc).
- **Demo mode**: simulate speed changes without GPS for testing.
- **Live status UI**: shows current speed, connection state, and debug info.
> Currently, the GPS speed shown on the app, and the status text of the state of connection with the ESP32 seems to be broken. I'm looking at how to fix it.

<img width="350" height="800" alt="image" src="https://github.com/user-attachments/assets/56e24b4d-dea6-4cab-a055-fbea1ea056bb" />
<img width="600" height="290" alt="image" src="https://github.com/user-attachments/assets/8a677121-4e2a-4970-a39c-8342920f4387" />

---

## Requirements (Which you have to manually enable from settings for now T_T)

- Android 12 or higher (tested on Android 15, One UI 7)
- ESP32 with [BluetoothSerial](https://github.com/espressif/arduino-esp32/tree/master/libraries/BluetoothSerial) enabled
- Paired device named `ESP32_HUD`
- Permissions:
  - `POST_NOTIFICATIONS`
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

---

## Bluetooth Protocol

The app sends simple text messages over Bluetooth SPP:

- **Speed values**: sent as floats:  
  ```text
  42.0\n
- **Speed limits**: prefixed with "LIMIT:" :  
  ```text
  LIMIT:80\n
---

## Downloads
You can just head over to actions and grab the latest artifact :p
