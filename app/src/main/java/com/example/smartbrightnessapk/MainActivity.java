package com.example.smartbrightnessapk;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.content.Context;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.Button;
import android.content.Intent;

public class MainActivity extends AppCompatActivity {
    // MQTT configuration constants
    private static final String BROKER_URL = "tcp://broker.hivemq.com:1883";
    private static final String DISTANCE_TOPIC = "ultrasonic/distance";
    private static final String CLIENT_ID = "Ultrasonic_MobileApp";

    // Distance classification constants
    private static final double SAFE_DISTANCE = 50.0; // cm
    private static final double ALERT_DISTANCE = 25.0; // cm
    private static final double DANGER_DISTANCE = 5.0; // cm

    // UI and MQTT components
    private MqttHandler mqttHandler;
    private TextView distanceText;
    private TextView statusText;
    private ImageView statusIcon;
    private LinearLayout statusIndicatorLayout;
    private boolean isMqttConnected = false;
    private Vibrator vibrator;

    // Distance classification enum
    public enum DistanceStatus {
        SAFE,
        ALERT,
        DANGER
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Initialize vibrator for alerts
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Initialize MQTT handler and connect to broker
        mqttHandler = new MqttHandler();
        mqttHandler.connect(BROKER_URL, CLIENT_ID, new MqttHandler.ConnectionListener() {
            @Override
            public void onConnected() {
                isMqttConnected = true;
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "MQTT Connected", Toast.LENGTH_SHORT).show();
                    subscribeToTopic(DISTANCE_TOPIC);
                });
            }

            @Override
            public void onConnectionFailed(Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "Connection Failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        });

        // Listen to incoming MQTT messages
        mqttHandler.setMessageListener((topic, message) -> {
            runOnUiThread(() -> {
                handleDistanceMessage(message);
            });
        });

        // Initialize UI elements
        distanceText = findViewById(R.id.distance_text);
        statusText = findViewById(R.id.status_text);
        statusIcon = findViewById(R.id.status_icon);
        statusIndicatorLayout = findViewById(R.id.status_indicator_layout);

        // Set up test button
        Button testButton = findViewById(R.id.test_button);
        testButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, TestActivity.class);
            startActivity(intent);
        });

        // Set initial state
        updateDistanceDisplay(0.0);

        // Insets for edge-to-edge layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // Handle incoming distance messages
    private void handleDistanceMessage(String message) {
        try {
            // Parse distance value from message
            // Expected format: "distance: 25.5" or just "25.5"
            String cleanMessage = message.replaceAll("[^0-9.]", "");
            double distance = Double.parseDouble(cleanMessage);
            
            updateDistanceDisplay(distance);
            
            // Classify distance and update UI
            DistanceStatus status = classifyDistance(distance);
            updateStatusDisplay(status);
            
            // Show debug info
            String debugInfo = String.format("Distance: %.1f cm → %s", distance, status.toString());
            Toast.makeText(this, debugInfo, Toast.LENGTH_SHORT).show();
            
            // Trigger vibration alert for danger zone
            if (status == DistanceStatus.DANGER) {
                triggerVibrationAlert();
            }
            
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid distance format: " + message, Toast.LENGTH_SHORT).show();
        }
    }

    // Classify distance into safety levels
    private DistanceStatus classifyDistance(double distance) {
        if (distance >= SAFE_DISTANCE) {
            return DistanceStatus.SAFE;
        } else if (distance >= ALERT_DISTANCE) {
            return DistanceStatus.ALERT;
        } else {
            return DistanceStatus.DANGER;
        }
    }

    // Update distance display
    private void updateDistanceDisplay(double distance) {
        distanceText.setText(String.format("%.1f cm", distance));
    }

    // Update status display with colors and icons
    private void updateStatusDisplay(DistanceStatus status) {
        switch (status) {
            case SAFE:
                statusText.setText("✅ Safe");
                statusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                statusIcon.setImageResource(android.R.drawable.ic_dialog_info);
                statusIndicatorLayout.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
                break;
            case ALERT:
                statusText.setText("⚠️ Be Alert");
                statusText.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                statusIcon.setImageResource(android.R.drawable.ic_dialog_alert);
                statusIndicatorLayout.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_light));
                break;
            case DANGER:
                statusText.setText("🚨 Danger!");
                statusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                statusIcon.setImageResource(android.R.drawable.ic_dialog_alert);
                statusIndicatorLayout.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
                break;
        }
    }

    // Trigger vibration alert for danger zone
    private void triggerVibrationAlert() {
        if (vibrator != null && vibrator.hasVibrator()) {
            // Create a vibration pattern: wait 0ms, vibrate 500ms, wait 200ms, vibrate 500ms
            long[] pattern = {0, 500, 200, 500};
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
        }
    }

    // Subscribe to MQTT topic
    private void subscribeToTopic(String topic) {
        Toast.makeText(this, "Subscribing to topic " + topic, Toast.LENGTH_SHORT).show();
        mqttHandler.subscribe(topic);
    }

    @Override
    protected void onDestroy() {
        // Disconnect MQTT connection when activity is destroyed
        mqttHandler.disconnect();
        super.onDestroy();
    }
}