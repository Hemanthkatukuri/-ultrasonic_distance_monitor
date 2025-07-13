# Ultrasonic Distance Monitor

An Android application that monitors ultrasonic distance sensor data via MQTT and provides real-time distance classification with visual and haptic alerts.

## Features

### 🎯 Core Functionality
- **Real-time Distance Monitoring**: Displays distance values from ultrasonic sensor in centimeters
- **Distance Classification**: Automatically classifies distances into safety zones:
  - ✅ **Safe**: ≥50 cm (Green)
  - ⚠️ **Be Alert**: 25-49.9 cm (Yellow) 
  - 🚨 **Danger**: <25 cm (Red)

### 🔔 Alert System
- **Visual Indicators**: Color-coded status display with icons
- **Haptic Feedback**: Vibration alerts when in danger zone
- **Real-time Updates**: Instant UI updates as distance changes

### 📡 MQTT Integration
- **Broker**: HiveMQ (tcp://broker.hivemq.com:1883)
- **Topic**: `ultrasonic/distance`
- **Message Format**: Simple numeric values (e.g., "25.5", "50", "10")

### 🧪 Testing Features
- **Built-in Simulator**: Test the app without an actual sensor
- **Distance Slider**: Adjust simulated distance values (0-100 cm)
- **Real-time Publishing**: Send test data to the same MQTT topic

## Setup Instructions

### 1. Build and Install
```bash
# Build the app
./gradlew assembleDebug

# Install on device
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 2. Configure Your Ultrasonic Sensor
Your sensor should publish distance data to the MQTT topic `ultrasonic/distance` with:
- **Broker**: `tcp://broker.hivemq.com:1883`
- **Topic**: `ultrasonic/distance`
- **Message Format**: Numeric distance in centimeters (e.g., "25.5")

### 3. Testing Without Sensor
1. Launch the app
2. Tap "Test Simulator" button
3. Adjust the distance slider
4. Tap "Publish Distance" to send test data
5. Return to main screen to see real-time updates

## Technical Details

### Distance Classification Logic
```java
if (distance >= 50.0) {
    return SAFE;      // Green indicator
} else if (distance >= 25.0) {
    return ALERT;     // Yellow indicator  
} else {
    return DANGER;    // Red indicator + vibration
}
```

### MQTT Configuration
- **Client ID**: `Ultrasonic_MobileApp`
- **Broker**: HiveMQ public broker
- **QoS**: Default (0)
- **Clean Session**: true

### Permissions
- `INTERNET`: For MQTT communication
- `VIBRATE`: For haptic alerts

## Customization

### Distance Thresholds
Modify the constants in `MainActivity.java`:
```java
private static final double SAFE_DISTANCE = 50.0;    // cm
private static final double ALERT_DISTANCE = 25.0;   // cm
private static final double DANGER_DISTANCE = 5.0;   // cm
```

### MQTT Topic
Change the topic in `MainActivity.java`:
```java
private static final String DISTANCE_TOPIC = "ultrasonic/distance";
```

### Vibration Pattern
Modify the vibration alert in `MainActivity.java`:
```java
long[] pattern = {0, 500, 200, 500}; // wait, vibrate, wait, vibrate
```

## Troubleshooting

### Connection Issues
- Ensure internet connectivity
- Check if HiveMQ broker is accessible
- Verify MQTT topic name matches your sensor

### No Distance Updates
- Verify sensor is publishing to correct topic
- Check message format (should be numeric)
- Ensure app has internet permission

### Vibration Not Working
- Check device vibration settings
- Ensure app has VIBRATE permission
- Test on physical device (emulator may not vibrate)

## Architecture

```
MainActivity (UI + MQTT Client)
    ↓
MqttHandler (MQTT Communication)
    ↓
HiveMQ Broker
    ↑
Ultrasonic Sensor (Your Hardware)
```

## Dependencies

- **Eclipse Paho MQTT**: For MQTT client functionality
- **AndroidX**: For modern Android components
- **ConstraintLayout**: For responsive UI design

