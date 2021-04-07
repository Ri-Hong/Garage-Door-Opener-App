# Garage-Door-Opener-App
**Note\* This app is the companion app of [Garage-Door-Opener](https://github.com/Ri-Hong/Garage-Door-Opener)**
## Description of Android Application
The app requires to tap the "ready" button then tap the "go" button. The two buttons are there to avoid accidental touches. The app then publishes a code onto the MQTT topic which can be read by the ESP01 Module. The app will display the last time it received a heartbeat message from the ESP01 module as well as the current time accurate to the second. After sending a request to open the door to the ESP01 module, it will display "Success" if the ESP01 module recognizes the code, "Failed" if the module does not recognize the code, and the actual message itself if the response is not a "0" or "1".

<img src="https://raw.githubusercontent.com/Ri-Hong/Garage-Door-Opener/main/Images/App_Screenshot.png?token=APROAV2WNYMW77ZRAQ3PHVS77NKA2" alt="Screenshot of app interface" width="200"/>
Feel free to contact me at riri.hong@gmail.com if you have any questions!