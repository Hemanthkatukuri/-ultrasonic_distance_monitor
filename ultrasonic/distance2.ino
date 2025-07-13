
#include <SPI.h>
#include <WiFi.h>
#include <WifiIPStack.h>
#include <Countdown.h>
#include <MQTTClient.h>
#include "Ultrasonic.h"

#define BUZZER_PIN     39       // Digital output to buzzer
#define LED_PIN        64       // Red onboard LED (D7)
#define ULTRASONIC_PIN 24       // Grove SIG pin
#define MQTTCLIENT_QOS2 2

// WiFi credentials
char ssid[] = "Hemanth";
char password[] = "123456789";

// MQTT topic and setup
const char* topic = "ultrasonic/distance";
char mqttServer[] = "broker.hivemq.com";
int mqttPort = 1883;

WifiIPStack ipstack;
MQTT::Client<WifiIPStack, Countdown> client = MQTT::Client<WifiIPStack, Countdown>(ipstack);
Ultrasonic ultrasonic(ULTRASONIC_PIN);

long lastDistance = -1;
unsigned long lastPublish = 0;
const int publishInterval = 1000;  // 1 second

void connectMQTT() {
  int rc = ipstack.connect(mqttServer, mqttPort);
  if (rc != 1) {
    Serial.print("TCP connect failed: ");
    Serial.println(rc);
    return;
  }

  MQTTPacket_connectData data = MQTTPacket_connectData_initializer;
  data.MQTTVersion = 3;
  data.clientID.cstring = (char*)"UltrasonicClient";

  rc = client.connect(data);
  if (rc != 0) {
    Serial.print("MQTT connect failed: ");
    Serial.println(rc);
    return;
  }

  Serial.println("MQTT Connected to HiveMQ Broker");
}

void setup() {
  Serial.begin(115200);

  pinMode(BUZZER_PIN, OUTPUT);
  digitalWrite(BUZZER_PIN, LOW);
  pinMode(LED_PIN, OUTPUT);
  digitalWrite(LED_PIN, HIGH);

  // Connect WiFi
  Serial.print("Connecting to WiFi: ");
  Serial.println(ssid);
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\nWiFi connected!");
  Serial.print("IP Address: ");
  Serial.println(WiFi.localIP());

  connectMQTT();
}

void loop() {
  long distance = ultrasonic.MeasureInCentimeters();

  // Send if changed or every second
  if (millis() - lastPublish > publishInterval || distance != lastDistance) {
    lastDistance = distance;
    lastPublish = millis();

    String status = "";
    if (distance >= 50) {
      status = "SAFE";
      digitalWrite(LED_PIN, LOW);
      digitalWrite(BUZZER_PIN, LOW);
    } else if (distance >= 25) {
      status = "BE ALERT";
      digitalWrite(LED_PIN, HIGH);
      digitalWrite(BUZZER_PIN, LOW);
    } else if (distance >= 5) {
      status = "DANGER";
      digitalWrite(LED_PIN, HIGH);
      digitalWrite(BUZZER_PIN, LOW);
    } else {
      status = "CRITICAL STOP";
      digitalWrite(LED_PIN, HIGH);
      digitalWrite(BUZZER_PIN, HIGH);
    }

    char payload[100];
    sprintf(payload, "Distance: %ld cm | Status: %s", distance, status.c_str());
    Serial.println(payload);

    if (!client.isConnected()) connectMQTT();

    MQTT::Message message;
    message.qos = MQTT::QOS0;
    message.retained = false;
    message.dup = false;
    message.payload = (void*)payload;
    message.payloadlen = strlen(payload) + 1;

    client.publish(topic, message);
  }

  client.yield(100);
}

