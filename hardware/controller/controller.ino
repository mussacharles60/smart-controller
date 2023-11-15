#include <SoftwareSerial.h>
#include <ArduinoJson.h>

SoftwareSerial bluetooth(3, 2); // RX & TX

String data = "";

void setup() {
  Serial.begin(9600);
  bluetooth.begin(9600);
}

void loop() {
  while (Serial.available() > 0) {
    char c = Serial.read();
    data += c;
    delay(10);
  }
  while (bluetooth.available() > 0) {
    char c = bluetooth.read();
    data += c;
    delay(10);
  }
  if (data.length() > 0) {
    data.trim();
    Serial.println(data);
    if (data.indexOf("start") >= 0) {
      delay(5000);
      sendFinish();
    }
    data = "";
  }
}

void sendFinish() {
  Serial.println(F("send: finish"));
  bluetooth.println(F("finish"));
}
