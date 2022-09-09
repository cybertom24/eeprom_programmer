#include <Arduino.h>
#line 1 "c:\\Users\\savol\\Documents\\CyberIndustries\\Progetti\\eeprom_programmer\\Arduino_test\\Arduino_test.ino"
#define CE_PIN A0
#define OE_PIN A1
#define WE_PIN A3

#define CE_MASK B00000001
#define OE_MASK B00000010
#define WE_MASK B00000100

#define SET_CE_PIN    B00000001   // CE_PIN is A0 so is PC0
#define RESET_CE_PIN  B11111110
#define SET_WE_PIN    B00001000   // WE_PIN is A3 so is PC3
#define RESET_WE_PIN  B11110111

#define CONTROL_DISABLE ( WE_MASK | OE_MASK |  CE_MASK)   // 111
#define CONTROL_ENABLE  ( WE_MASK | OE_MASK           )   // 110
#define CONTROL_READ    ( WE_MASK                     )   // 100
#define CONTROL_WRITE   (           OE_MASK           )   // 010
#define CONTROL_SDP     (           OE_MASK |  CE_MASK)   // 011

#define DELAY 2
#define DELAY_WRITING 15

#line 23 "c:\\Users\\savol\\Documents\\CyberIndustries\\Progetti\\eeprom_programmer\\Arduino_test\\Arduino_test.ino"
void setup();
#line 29 "c:\\Users\\savol\\Documents\\CyberIndustries\\Progetti\\eeprom_programmer\\Arduino_test\\Arduino_test.ino"
void loop();
#line 33 "c:\\Users\\savol\\Documents\\CyberIndustries\\Progetti\\eeprom_programmer\\Arduino_test\\Arduino_test.ino"
void setControlBits(uint8_t state);
#line 39 "c:\\Users\\savol\\Documents\\CyberIndustries\\Progetti\\eeprom_programmer\\Arduino_test\\Arduino_test.ino"
void pulse(byte pin, boolean state);
#line 71 "c:\\Users\\savol\\Documents\\CyberIndustries\\Progetti\\eeprom_programmer\\Arduino_test\\Arduino_test.ino"
void q_pulse(byte pin, boolean type);
#line 95 "c:\\Users\\savol\\Documents\\CyberIndustries\\Progetti\\eeprom_programmer\\Arduino_test\\Arduino_test.ino"
void write(uint16_t address, uint8_t data);
#line 23 "c:\\Users\\savol\\Documents\\CyberIndustries\\Progetti\\eeprom_programmer\\Arduino_test\\Arduino_test.ino"
void setup() {
    pinMode(CE_PIN, OUTPUT);
    pinMode(WE_PIN, OUTPUT);
    pinMode(OE_PIN, OUTPUT);
}

void loop() {
    write(0, 0);
}

void setControlBits(uint8_t state) {
    digitalWrite(CE_PIN, state & CE_MASK);
    digitalWrite(OE_PIN, state & CE_MASK);
    digitalWrite(WE_PIN, state & CE_MASK);
}

void pulse(byte pin, boolean state) {
    if(pin == WE_PIN) {
        if(state) {
            PORTC |= SET_WE_PIN;
            delayMicroseconds(DELAY);
            PORTC &= RESET_WE_PIN;
        }
        else {
            PORTC &= RESET_WE_PIN;
            delayMicroseconds(DELAY);
            PORTC |= SET_WE_PIN;
        }
    }
    else if(pin == CE_PIN) {
        if(state) {
            PORTC |= SET_CE_PIN ;
            delayMicroseconds(DELAY);
            PORTC &= RESET_CE_PIN;
        }
        else {
            PORTC &= RESET_CE_PIN;
            delayMicroseconds(DELAY);
            PORTC |= SET_CE_PIN ;
        }
    }
    else {
        digitalWrite(pin, state);
        delayMicroseconds(DELAY);
        digitalWrite(pin, !state);
    }
}

void q_pulse(byte pin, boolean type) {
  uint8_t set = 0, reset = 0;
  switch(pin) {
    case WE_PIN:
      set = SET_WE_PIN;
      reset = RESET_WE_PIN;
      break;
    case CE_PIN:
      set = SET_CE_PIN;
      reset = RESET_CE_PIN;
      break;
  }

  if(type) {
    PORTC |= set;
    delayMicroseconds(DELAY);
    PORTC &= reset;
  } else {
    PORTC &= reset;
    delayMicroseconds(DELAY);
    PORTC |= set;
  }
}

void write(uint16_t address, uint8_t data) {  
  setControlBits(CONTROL_DISABLE);
  
  // Imposta l'address
  //q_setAddress(address);
  // Imposta il dato
  //q_putOnDataPin(data);
  // Dai il segnale di scrittura
  q_pulse(CE_PIN, LOW);
  q_pulse(WE_PIN, LOW);
  // Aspetta il tempo che scriva
  delay(DELAY_WRITING);
}
