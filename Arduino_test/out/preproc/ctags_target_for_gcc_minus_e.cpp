# 1 "c:\\Users\\savol\\Documents\\CyberIndustries\\Progetti\\eeprom_programmer\\Arduino_test\\Arduino_test.ino"
# 23 "c:\\Users\\savol\\Documents\\CyberIndustries\\Progetti\\eeprom_programmer\\Arduino_test\\Arduino_test.ino"
void setup() {
    pinMode(A0, 0x1);
    pinMode(A3, 0x1);
    pinMode(A1, 0x1);
}

void loop() {
    write(0, 0);
}

void setControlBits(uint8_t state) {
    digitalWrite(A0, state & 1);
    digitalWrite(A1, state & 1);
    digitalWrite(A3, state & 1);
}

void pulse(byte pin, boolean state) {
    if(pin == A3) {
        if(state) {
            
# 42 "c:\\Users\\savol\\Documents\\CyberIndustries\\Progetti\\eeprom_programmer\\Arduino_test\\Arduino_test.ino" 3
           (*(volatile uint8_t *)((0x08) + 0x20)) 
# 42 "c:\\Users\\savol\\Documents\\CyberIndustries\\Progetti\\eeprom_programmer\\Arduino_test\\Arduino_test.ino"
                 |= 8 /* WE_PIN is A3 so is PC3*/;
            delayMicroseconds(2);
            
# 44 "c:\\Users\\savol\\Documents\\CyberIndustries\\Progetti\\eeprom_programmer\\Arduino_test\\Arduino_test.ino" 3
           (*(volatile uint8_t *)((0x08) + 0x20)) 
# 44 "c:\\Users\\savol\\Documents\\CyberIndustries\\Progetti\\eeprom_programmer\\Arduino_test\\Arduino_test.ino"
                 &= 247;
        }
        else {
            
# 47 "c:\\Users\\savol\\Documents\\CyberIndustries\\Progetti\\eeprom_programmer\\Arduino_test\\Arduino_test.ino" 3
           (*(volatile uint8_t *)((0x08) + 0x20)) 
# 47 "c:\\Users\\savol\\Documents\\CyberIndustries\\Progetti\\eeprom_programmer\\Arduino_test\\Arduino_test.ino"
                 &= 247;
            delayMicroseconds(2);
            
# 49 "c:\\Users\\savol\\Documents\\CyberIndustries\\Progetti\\eeprom_programmer\\Arduino_test\\Arduino_test.ino" 3
           (*(volatile uint8_t *)((0x08) + 0x20)) 
# 49 "c:\\Users\\savol\\Documents\\CyberIndustries\\Progetti\\eeprom_programmer\\Arduino_test\\Arduino_test.ino"
                 |= 8 /* WE_PIN is A3 so is PC3*/;
        }
    }
    else if(pin == A0) {
        if(state) {
            
# 54 "c:\\Users\\savol\\Documents\\CyberIndustries\\Progetti\\eeprom_programmer\\Arduino_test\\Arduino_test.ino" 3
           (*(volatile uint8_t *)((0x08) + 0x20)) 
# 54 "c:\\Users\\savol\\Documents\\CyberIndustries\\Progetti\\eeprom_programmer\\Arduino_test\\Arduino_test.ino"
                 |= 1 /* CE_PIN is A0 so is PC0*/ ;
            delayMicroseconds(2);
            
# 56 "c:\\Users\\savol\\Documents\\CyberIndustries\\Progetti\\eeprom_programmer\\Arduino_test\\Arduino_test.ino" 3
           (*(volatile uint8_t *)((0x08) + 0x20)) 
# 56 "c:\\Users\\savol\\Documents\\CyberIndustries\\Progetti\\eeprom_programmer\\Arduino_test\\Arduino_test.ino"
                 &= 254;
        }
        else {
            
# 59 "c:\\Users\\savol\\Documents\\CyberIndustries\\Progetti\\eeprom_programmer\\Arduino_test\\Arduino_test.ino" 3
           (*(volatile uint8_t *)((0x08) + 0x20)) 
# 59 "c:\\Users\\savol\\Documents\\CyberIndustries\\Progetti\\eeprom_programmer\\Arduino_test\\Arduino_test.ino"
                 &= 254;
            delayMicroseconds(2);
            
# 61 "c:\\Users\\savol\\Documents\\CyberIndustries\\Progetti\\eeprom_programmer\\Arduino_test\\Arduino_test.ino" 3
           (*(volatile uint8_t *)((0x08) + 0x20)) 
# 61 "c:\\Users\\savol\\Documents\\CyberIndustries\\Progetti\\eeprom_programmer\\Arduino_test\\Arduino_test.ino"
                 |= 1 /* CE_PIN is A0 so is PC0*/ ;
        }
    }
    else {
        digitalWrite(pin, state);
        delayMicroseconds(2);
        digitalWrite(pin, !state);
    }
}

void q_pulse(byte pin, boolean type) {
  uint8_t set = 0, reset = 0;
  switch(pin) {
    case A3:
      set = 8 /* WE_PIN is A3 so is PC3*/;
      reset = 247;
      break;
    case A0:
      set = 1 /* CE_PIN is A0 so is PC0*/;
      reset = 254;
      break;
  }

  if(type) {
    
# 85 "c:\\Users\\savol\\Documents\\CyberIndustries\\Progetti\\eeprom_programmer\\Arduino_test\\Arduino_test.ino" 3
   (*(volatile uint8_t *)((0x08) + 0x20)) 
# 85 "c:\\Users\\savol\\Documents\\CyberIndustries\\Progetti\\eeprom_programmer\\Arduino_test\\Arduino_test.ino"
         |= set;
    delayMicroseconds(2);
    
# 87 "c:\\Users\\savol\\Documents\\CyberIndustries\\Progetti\\eeprom_programmer\\Arduino_test\\Arduino_test.ino" 3
   (*(volatile uint8_t *)((0x08) + 0x20)) 
# 87 "c:\\Users\\savol\\Documents\\CyberIndustries\\Progetti\\eeprom_programmer\\Arduino_test\\Arduino_test.ino"
         &= reset;
  } else {
    
# 89 "c:\\Users\\savol\\Documents\\CyberIndustries\\Progetti\\eeprom_programmer\\Arduino_test\\Arduino_test.ino" 3
   (*(volatile uint8_t *)((0x08) + 0x20)) 
# 89 "c:\\Users\\savol\\Documents\\CyberIndustries\\Progetti\\eeprom_programmer\\Arduino_test\\Arduino_test.ino"
         &= reset;
    delayMicroseconds(2);
    
# 91 "c:\\Users\\savol\\Documents\\CyberIndustries\\Progetti\\eeprom_programmer\\Arduino_test\\Arduino_test.ino" 3
   (*(volatile uint8_t *)((0x08) + 0x20)) 
# 91 "c:\\Users\\savol\\Documents\\CyberIndustries\\Progetti\\eeprom_programmer\\Arduino_test\\Arduino_test.ino"
         |= set;
  }
}

void write(uint16_t address, uint8_t data) {
  setControlBits(( 4 | 2 | 1) /* 111*/);

  // Imposta l'address
  //q_setAddress(address);
  // Imposta il dato
  //q_putOnDataPin(data);
  // Dai il segnale di scrittura
  q_pulse(A0, 0x0);
  q_pulse(A3, 0x0);
  // Aspetta il tempo che scriva
  delay(15);
}
