/*
UDP Controll LED.
No change necessary.
*/

#define DEBUG true

#define LED 9
#include <Arduino.h>
#include <SoftwareSerial.h>
#include <Servo.h>
Servo servo1;
int position1 = 0;
//Servo servo2;

SoftwareSerial esp8266(11, 12); // RX, TX

void setup() {
  servo1.attach (10);
  
  Serial.begin(19200);
  esp8266.begin(19200);
  
  esp8266.setTimeout(5000);
  if (sendCom("AT+RST", "ready")) {
    debug("RESET OK");
  }

  if (configAP()) {
    debug("AP ready");
  }
  if (configUDP()) {
    debug("UDP ready");
  }
  
  //shorter Timeout for faster wrong UPD-Comands handling
    esp8266.setTimeout(1000);
   
    pinMode(LED, OUTPUT);
    
}

void loop() {
  
  if (esp8266.available())
  {
    Serial.println("esp ready");
    if (esp8266.find("+IPD,"))
    {
      Serial.println("Here");
//      if (esp8266.find(":")) {
      String task = esp8266.readStringUntil('\r');
      Serial.println("task");
      
      if (task.indexOf("led0") >= 0) {
        int setLed = 0;
        digitalWrite(LED, setLed);

        debug("LED=" + String(setLed));
        if (sendCom("AT+CIPSEND=7", ">"))
        {
          sendCom("LED=" + String(setLed), "OK");
        }
      } else if (task.indexOf("led1") >= 0) {
        int setLed = 1;
        digitalWrite(LED, setLed);

        debug("LED=" + String(setLed));
        if (sendCom("AT+CIPSEND=7", ">"))
        {
          sendCom("LED=" + String(setLed), "OK");
        }
      }
      else if (task.indexOf("motorOn") >= 0) {
        debug("motor1=on");
        if (position1 >= 180) {
          position1 = 0;
        }
        while (position1 < 180) {
          servo1.write(position1);
          delay(30);
          position1++;
        }
        position1 = 0;   
      } else {
        debug("Wrong UDP Command");
        if (sendCom("AT+CIPSEND=19", ">"))
        {
          sendCom("Wrong UDP Command", "OK");
        }
      }
    }
  }

}

//-----------------------------------------Config ESP8266------------------------------------

boolean configAP()
{
  boolean success = true;

  success &= (sendCom("AT+CWMODE=2", "OK"));
  success &= (sendCom("AT+CWSAP=\"NanoESP\",\"\",5,0", "OK"));

  return success;
}

boolean configUDP()
{
  boolean success = true;

  success &= (sendCom("AT+CIPMODE=0", "OK"));
  success &= (sendCom("AT+CIPMUX=0", "OK"));
  success &= sendCom("AT+CIPSTART=\"UDP\",\"192.168.4.255\",55056,55057", "OK"); //UDP Bidirectional and Broadcast
  return success;
}

//-----------------------------------------------Controll ESP-----------------------------------------------------

boolean sendCom(String command, char respond[])
{
  esp8266.println(command);
  if (esp8266.findUntil(respond, "ERROR"))
  {
    return true;
  }
  else
  {
    debug("ESP SEND ERROR: " + command);
    return false;
  }
}

String sendCom(String command)
{
  esp8266.println(command);
  return esp8266.readString();
}

//-------------------------------------------------Debug Functions------------------------------------------------------
void serialDebug() {
  while (true)
  {
    if (esp8266.available())
      Serial.write(esp8266.read());
    if (Serial.available())
      esp8266.write(Serial.read());
  }
}

void debug(String Msg)
{
  if (DEBUG)
  {
    Serial.println(Msg);
  }
}
