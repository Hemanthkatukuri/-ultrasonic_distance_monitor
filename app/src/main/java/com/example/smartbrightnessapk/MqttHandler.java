package com.example.smartbrightnessapk;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttHandler {

    private MqttClient client; // MQTT client instance
    private MessageListener messageListener; // Custom message listener interface
    // Set a message listener for incoming messages
    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }
   // Connect to MQTT broker with specified client ID and notify listener on status
    public void connect(String brokerUrl, String clientId, ConnectionListener listener) {
        try {
            MemoryPersistence persistence = new MemoryPersistence(); // Use in-memory persistence
            client = new MqttClient(brokerUrl, clientId, persistence); // Initialize MQTT client

            MqttConnectOptions connectOptions = new MqttConnectOptions();
            connectOptions.setCleanSession(true); // Start a clean session each time

            client.connect(connectOptions); // Connect to broker
            if (listener != null) {
                listener.onConnected(); // Notify success
            }
        } catch (MqttException e) {
            e.printStackTrace(); // Print exception if connection fails
            if (listener != null) {
                listener.onConnectionFailed(e); // Notify failure
            }
        }
    }
    // Disconnect from MQTT broker
    public void disconnect() {
        try {
            client.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    // Publish a message to a topic
    public void publish(String topic, String message) {
        try {
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());  // Create message from string
            client.publish(topic, mqttMessage);  // Publish to topic
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    // Subscribe to a topic and handle incoming messages via callback
    public void subscribe(String topic) {
        try {
            client.subscribe(topic, (t, mqttMessage) -> {
                if (messageListener != null) {
                    // Pass the topic and message to the listener
                    messageListener.onMessageReceived(t, new String(mqttMessage.getPayload()));
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    // Interface for connection status callbacks
    public interface ConnectionListener {
        void onConnected();
        void onConnectionFailed(Exception e);
    }
    // Interface for message reception callbacks
    public interface MessageListener {
        void onMessageReceived(String topic, String message);
    }

}
