package com.example.smartbrightnessapk;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttHandler {

    private MqttClient client;
    private MessageListener messageListener;
    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }

    public void connect(String brokerUrl, String clientId, ConnectionListener listener) {
        try {
            MemoryPersistence persistence = new MemoryPersistence();
            client = new MqttClient(brokerUrl, clientId, persistence);

            MqttConnectOptions connectOptions = new MqttConnectOptions();
            connectOptions.setCleanSession(true);

            client.connect(connectOptions);
            if (listener != null) {
                listener.onConnected();
            }
        } catch (MqttException e) {
            e.printStackTrace();
            if (listener != null) {
                listener.onConnectionFailed(e);
            }
        }
    }

    public void disconnect() {
        try {
            client.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }



    public void publish(String topic, String message) {
        try {
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            client.publish(topic, mqttMessage);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void subscribe(String topic) {
        try {
            client.subscribe(topic, (t, mqttMessage) -> {
                if (messageListener != null) {
                    messageListener.onMessageReceived(t, new String(mqttMessage.getPayload()));
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public interface ConnectionListener {
        void onConnected();
        void onConnectionFailed(Exception e);
    }
    public interface MessageListener {
        void onMessageReceived(String topic, String message);
    }

}