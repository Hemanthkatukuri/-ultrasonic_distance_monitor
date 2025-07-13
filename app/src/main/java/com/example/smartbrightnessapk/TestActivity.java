package com.example.smartbrightnessapk;

import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class TestActivity extends AppCompatActivity {
    
    private SeekBar distanceSeekBar;
    private TextView distanceValueText;
    private Button publishButton;
    private MqttHandler mqttHandler;
    private static final String TEST_TOPIC = "ultrasonic/distance";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        // Initialize MQTT handler
        mqttHandler = new MqttHandler();
        mqttHandler.connect("tcp://broker.hivemq.com:1883", "TestClient", new MqttHandler.ConnectionListener() {
            @Override
            public void onConnected() {
                runOnUiThread(() -> {
                    Toast.makeText(TestActivity.this, "MQTT Connected for Testing", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onConnectionFailed(Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(TestActivity.this, "Test Connection Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });

        // Initialize UI elements
        distanceSeekBar = findViewById(R.id.distance_seekbar);
        distanceValueText = findViewById(R.id.distance_value_text);
        publishButton = findViewById(R.id.publish_button);

        // Set up seek bar
        distanceSeekBar.setMax(100); // 0-100 cm
        distanceSeekBar.setProgress(50); // Start at 50 cm
        updateDistanceDisplay(50);

        distanceSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateDistanceDisplay(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Set up publish button
        publishButton.setOnClickListener(v -> {
            int distance = distanceSeekBar.getProgress();
            String message = String.valueOf(distance);
            mqttHandler.publish(TEST_TOPIC, message);
            Toast.makeText(this, "Published: " + message + " cm", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateDistanceDisplay(int distance) {
        distanceValueText.setText(distance + " cm");
        
        // Show zone classification
        String zone;
        if (distance >= 50) {
            zone = "✅ Safe";
        } else if (distance >= 25) {
            zone = "⚠️ Alert";
        } else {
            zone = "🚨 Danger";
        }
        
        distanceValueText.setText(distance + " cm (" + zone + ")");
        
        // Show debug info
        String debugInfo = String.format("Distance: %d cm → %s", distance, zone);
        Toast.makeText(this, debugInfo, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        mqttHandler.disconnect();
        super.onDestroy();
    }
} 