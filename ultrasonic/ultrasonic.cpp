#include "Arduino.h"
#include "Ultrasonic.h"

Ultrasonic::Ultrasonic(int pin) {
  _pin = pin;
  pinMode(_pin, OUTPUT);
  digitalWrite(_pin, LOW);
}

long Ultrasonic::MeasureInCentimeters() {
  // Generate trigger pulse
  digitalWrite(_pin, HIGH);
  delayMicroseconds(10);  // Grove requires 10μs pulse
  digitalWrite(_pin, LOW);
  
  // Measure echo
  pinMode(_pin, INPUT);
  long duration = pulseIn(_pin, HIGH, 30000);  // 30ms timeout (5m range)
  pinMode(_pin, OUTPUT);
  
  // Return distance in cm (0 for timeout/error)
  return duration > 0 ? duration / 29 / 2 : 0;
}