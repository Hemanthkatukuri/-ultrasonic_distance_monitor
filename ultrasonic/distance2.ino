#include <SPI.h>                      // SPI library for communication
#include <WiFi.h>                     // WiFi library for CC3200
#include <WifiIPStack.h>              // MQTT stack over WiFi for Energia
#include <Countdown.h>                // Countdown timer used by MQTT
#include <MQTTClient.h>               // MQTT client library
#include "Ultrasonic.h"               // Ultrasonic sensor custom library

// GPIO Pin Definitions
#define BUZZER_PIN     39             // Pin to which buzzer is connected
#define LED_PIN        64             // Pin for Red Onboard LED (D7 - active low)
#define ULTRASONIC_PIN 24             // Ultrasonic SIG pin connected to pin 24 (J15)
#define MQTTCLIENT_QOS2 2             // MQTT Quality of Service (QoS) level

// WiFi credentials (replace with your own network details)
char ssid[] = "Hemanth";              // WiFi SSID
char password[] = "123456789";        // WiFi password

// MQTT Configuration
const char* topic = "ultrasonic/distance"; // MQTT topic to publish data to
char mqttServer[] = "broker.hivemq.com";   // Public MQTT broker address
int mqttPort = 1883;                       // Standard MQTT port

// MQTT Network Stack
WifiIPStack ipstack;                   // Handles TCP/IP stack over WiFi
MQTT::Client<WifiIPStack, Countdown> client = MQTT::Client<WifiIPStack, Countdown>(ipstack);

// Ultrasonic Sensor Setup
Ultrasonic ultrasonic(ULTRASONIC_PIN); // Initialize ultrasonic sensor on specified pin

// Timing and distance tracking
long lastDistance = -1;                // Store last measured distance
unsigned long lastPublish = 0;         // Timestamp of last publish
const int publishInterval = 1000;      // Time interval (1s) between publishes

// Function to connect to MQTT Broker
void connectMQTT() {
  int rc = ipstack.connect(mqttServer, mqttPort); // Establish TCP connection to broker
  if (rc != 1) {
    Serial.print("TCP connect failed: ");
    Serial.println(rc);
    return;
  }

  // Configure MQTT connection parameters
  MQTTPacket_connectData data = MQTTPacket_connectData_initializer;
  data.MQTTVersion = 3;
  data.clientID.cstring = (char*)"UltrasonicClient";

  rc = client.connect(data);          // Connect MQTT client
  if (rc != 0) {
    Serial.print("MQTT connect failed: ");
    Serial.println(rc);
    return;
  }

  Serial.println("MQTT Connected to HiveMQ Broker");
}

void setup() {
  Serial.begin(115200);               // Start serial communication

  // Set up buzzer and LED as outputs
  pinMode(BUZZER_PIN, OUTPUT);
  digitalWrite(BUZZER_PIN, LOW);      // Ensure buzzer is off initially
  pinMode(LED_PIN, OUTPUT);
  digitalWrite(LED_PIN, HIGH);        // LED OFF (active low)

  // Connect to WiFi
  Serial.print("Connecting to WiFi: ");
  Serial.println(ssid);
  WiFi.begin(ssid, password);         // Initiate WiFi connection
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\nWiFi connected!");
  Serial.print("IP Address: ");
  Serial.println(WiFi.localIP());     // Print local IP once connected

  connectMQTT();                      // Connect to MQTT broker
}

void loop() {
  long distance = ultrasonic.MeasureInCentimeters(); // Measure distance from sensor

  // Publish data if time elapsed or distance changed
  if (millis() - lastPublish > publishInterval || distance != lastDistance) {
    lastDistance = distance;
    lastPublish = millis();

    // Determine status and update buzzer/LED based on distance
    String status = "";
    if (distance >= 50) {
      status = "SAFE";
      digitalWrite(LED_PIN, LOW);     // LED ON (active low)
      digitalWrite(BUZZER_PIN, LOW);  // Buzzer OFF
    } else if (distance >= 25) {
      status = "BE ALERT";
      digitalWrite(LED_PIN, HIGH);    // LED OFF
      digitalWrite(BUZZER_PIN, LOW);  // Buzzer OFF
    } else if (distance >= 5) {
      status = "DANGER";
      digitalWrite(LED_PIN, HIGH);    // LED OFF
      digitalWrite(BUZZER_PIN, LOW);  // Buzzer OFF
    } else {
      status = "CRITICAL STOP";
      digitalWrite(LED_PIN, HIGH);    // LED OFF
      digitalWrite(BUZZER_PIN, HIGH); // Buzzer ON
    }

    // Create MQTT message payload
    char payload[100];
    sprintf(payload, "Distance: %ld cm | Status: %s", distance, status.c_str());
    Serial.println(payload);          // Print to Serial monitor

    // Reconnect if MQTT is not connected
    if (!client.isConnected()) connectMQTT();

    // Publish message to MQTT broker
    MQTT::Message message;
    message.qos = MQTT::QOS0;
    message.retained = false;
    message.dup = false;
    message.payload = (void*)payload;
    message.payloadlen = strlen(payload) + 1;

    client.publish(topic, message);   // Publish data to defined topic
  }

  client.yield(100);                  // Handle MQTT keep-alive
}
