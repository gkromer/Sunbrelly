/*
UDP Controll LED.
No change necessary.
*/

#define DEBUG true

#define LED 9
#include <Arduino.h>
#include <SoftwareSerial.h>

//Servo servo2;

SoftwareSerial esp8266(11, 12); // RX, TX

int motorPin1=2;
int motorPin2=3; // PWM
int motor2Pin1=4;
int motor2Pin2=5; // PWM

void setup() {

  pinMode(motorPin1,OUTPUT);
  pinMode(motorPin2,OUTPUT);
   pinMode(motor2Pin1,OUTPUT);
  pinMode(motor2Pin2,OUTPUT);
  
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
//  //Fading the LED
//  for(int i=0; i<255; i++){
//    analogWrite(LED, i);
//    delay(5);
//  }
//
//  for(int i=255; i>0; i--){
//    analogWrite(LED, i);
//    delay(5);
//  }
  
  if (esp8266.available())
  {
    Serial.println("ESP ready");
    if (esp8266.find("+IPD,"))
    {
      Serial.println("command received");
      String task = esp8266.readStringUntil('\r');
      Serial.println(task);
      
      if (task.indexOf("led0") >= 0) {
        toggleLED(0);
      } else if (task.indexOf("led1") >= 0) {
       toggleLED(1);
      } else if (task.indexOf("motor1Forwards") >= 0) {
            digitalWrite(motorPin1,LOW);   // Motor Vor
            digitalWrite(motorPin2,HIGH);
            delay(1000);
            motorStop(1);                    // Motor Stop
      } else if (task.indexOf("motor1Backwards") >= 0) {
            digitalWrite(motorPin1,HIGH);   // Motor Vor
            digitalWrite(motorPin2,LOW);
            delay(1000);
            motorStop(1); 
      } else if (task.indexOf("motor1Stop") >= 0) {
            motorStop(1); 
      } else if (task.indexOf("motor1ForwardWithSignal") >= 0) {  //das Signal enthält die gewünschte Geschwindigkeit
        String speedString = task.substring((task.indexOf("motor1ForwardWithSignal") + 23), task.indexOf("motor1ForwardWithSignal") + 26);
        Serial.println("SPEED: " + speedString);
        int speed = speedString.toInt();
            digitalWrite(motorPin1, LOW);   // Motor langsam zu schnell
            analogWrite(motorPin2, speed);      
      } else if (task.indexOf("motor1BackwardWithSignal") >= 0) {  //das Signal enthält die gewünschte Geschwindigkeit
        String speedString = task.substring((task.indexOf("motor1BackwardWithSignal") + 24), task.indexOf("motor1BackwardWithSignal") + 27);
        Serial.println("SPEED: " + speedString);
        int speed = speedString.toInt();
            digitalWrite(motorPin1, speed);   
            analogWrite(motorPin2, LOW);  // Motor langsam zu schnell 
      } else if (task.indexOf("motor2Forwards") >= 0) {
            digitalWrite(motor2Pin1,LOW);   // Motor Vor
            digitalWrite(motor2Pin2,HIGH);
            delay(1000);
            motorStop(2);                    // Motor Stop
      } else if (task.indexOf("motor2Backwards") >= 0) {
            digitalWrite(motor2Pin1,HIGH);   // Motor Vor
            digitalWrite(motor2Pin2,LOW);
            delay(1000);
            motorStop(2); 
      } else if (task.indexOf("motor2Stop") >= 0) {
            motorStop(2); 
      } else if (task.indexOf("motor2ForwardWithSignal") >= 0) {  //das Signal enthält die gewünschte Geschwindigkeit
        String speedString = task.substring((task.indexOf("motor2ForwardWithSignal") + 23), task.indexOf("motor2ForwardWithSignal") + 26);
        Serial.println("SPEED: " + speedString);
        int speed = speedString.toInt();
            digitalWrite(motor2Pin1, LOW);   // Motor langsam zu schnell
            analogWrite(motor2Pin2, speed);      
      } else if (task.indexOf("motor2BackwardWithSignal") >= 0) {  //das Signal enthält die gewünschte Geschwindigkeit
        String speedString = task.substring((task.indexOf("motor2BackwardWithSignal") + 24), task.indexOf("motor2BackwardWithSignal") + 27);
        Serial.println("SPEED: " + speedString);
        int speed = speedString.toInt();
            digitalWrite(motor2Pin1, speed);   
            analogWrite(motor2Pin2, LOW);  // Motor langsam zu schnell 
      } else {
        debug("Wrong UDP Command");
        sendUDP("Wrong UDP Command");
        if (sendCom("AT+CIPSEND=19", ">"))
        {
          sendCom("Wrong UDP Command", "OK");
        }
      }
    }
  }

}

void toggleLED(int setLED) {
        digitalWrite(LED, setLED);
        debug("LED=" + String(setLED));
        sendUDP("LED=" + String(setLED));
        if (sendCom("AT+CIPSEND=7", ">"))
        {
          sendCom("LED=" + String(setLED), "OK");
        }
        
}

void motorStop(int motorNumber){
  switch (motorNumber ) {
    case 1:
    digitalWrite(motorPin1,LOW);
    digitalWrite(motorPin2,LOW);
    break;
    case 2:{
    digitalWrite(motor2Pin1,LOW);
    digitalWrite(motor2Pin2,LOW);
    break;
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

boolean sendUDP(String Msg)
{
  boolean success = true;

  success &= sendCom("AT+CIPSEND=" + String(Msg.length() + 2), ">");   
  if (success)
  {
    success &= sendCom(Msg, "OK");
  }
  return success;
}

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
