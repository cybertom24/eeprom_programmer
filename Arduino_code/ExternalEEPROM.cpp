#include "ExternalEEPROM.h"

/*
   Costructor. It needs all of the pins used
*/
ExternalEEPROM::ExternalEEPROM() {
  /* Set pin mode */
  pinMode(SER_PIN, OUTPUT);
  pinMode(SCK_PIN, OUTPUT);
  pinMode(RCK_PIN, OUTPUT);
  pinMode(G_PIN, OUTPUT);
  pinMode(SCL_PIN, OUTPUT);

  pinMode(CE_PIN, OUTPUT);
  pinMode(OE_PIN, OUTPUT);
  pinMode(WE_PIN, OUTPUT);

  setMode(READING);


  /* Pin status initialization */
  digitalWrite(SER_PIN, LOW);
  digitalWrite(SCK_PIN, LOW);
  digitalWrite(RCK_PIN, LOW);
  digitalWrite(G_PIN, LOW);
  digitalWrite(SCL_PIN, HIGH);

  setControlBits(CONTROL_DISABLE);
}

/*
   Method to read data contained in a particular address
*/
uint8_t ExternalEEPROM::read(uint16_t address) {
  // Assicurati che OE sia attivo (low) e WE disattivo
  setControlBits(CONTROL_READ);

  // Poni gli address in uscita
  q_setAddress(address);
  //delayMicroseconds(DELAY_READING);
  // Leggi il dato
  uint8_t data = readDataPin();

  // Resetta
  setControlBits(CONTROL_DISABLE);
  return data;
}

/*
   Write on the EEPROM the byte passed at a specified address
   Duration pre-delay: ~475us (media con 192 campioni) (max: 484us)
*/
void ExternalEEPROM::write(uint16_t address, uint8_t data) {  
  setControlBits(CONTROL_ENABLE);
  
  // Imposta l'address
  q_setAddress(address);
  // Imposta il dato
  q_putOnDataPin(data);
  // Dai il segnale di scrittura
  pulse(WE_PIN, LOW);
  // Aspetta il tempo che scriva
  delay(DELAY_WRITING);

  // Reset
  setControlBits(CONTROL_DISABLE);
}

/*
   Method to write an array of bytes into a specific page of the memory starting from a specific part of the page.
   The page is automatically identified using the starting address passed as an argument.
   Any additional byte will be ignored.
   Returns: Number of bytes written

   INUTILE: troppo lento il setup degli address (trovare il modo di velocizzarlo)
*/
int ExternalEEPROM::pageWrite(uint16_t address, int length, uint8_t* data) {
  // Recupera l'id della pagina
  uint16_t pageId = address & PAGE_ID_MASK;
  // Recupera la posizione all'interno della pagina
  uint16_t pagePos = address & PAGE_POS_MASK;
  // Controlla che la lunghezza non ecceda la pagina
  if (length > MAX_WRITING_PAGE_LENGTH - pagePos)
    length = MAX_WRITING_PAGE_LENGTH - pagePos;

  Serial.print("Wrinting on page 0x");
  Serial.print(pageId, HEX);
  Serial.print(" starting from 0x");
  Serial.print(pagePos, HEX);
  Serial.print(" with a length of ");
  Serial.println(length, DEC);  
  
  setControlBits(CONTROL_DISABLE);

  for (uint16_t i = 0; i < length; i++) {
    uint16_t addr = pageId + pagePos + i;
    
    q_setAddress(addr);
    putOnDataPin(data[i]);

    setControlBits(CONTROL_WRITE);
    // The delay is managed inside the method setControlBits
    setControlBits(CONTROL_DISABLE);

    delayMicroseconds(DELAY_PAGE_WRITING);
  }
  delay(DELAY_WRITING);

  // Reset
  setControlBits(CONTROL_DISABLE);
  
  return length;
}


void ExternalEEPROM::write(uint16_t address, int length, uint8_t* data) {
  for(int i = 0; i < length; i++) {
    write(address + i, data[i]);
  }
}

//void ExternalEEPROM::write(uint16_t address, int length, uint8_t* data) {
//  int i = 0;
//  while(i < length) {
//    // Scrivi e aggiona i dei byte che sono stati scritti (e quindi cambia pagina)
//    i += pageWrite(address + i, length - i, &data[i]);
//  }  
//}

/*
   Remember to free the memory after usage
*/
uint8_t* ExternalEEPROM::read(uint16_t startAddress, uint8_t length) {
  if(length > MAX_READING_BUFFER_LENGTH)
    length = MAX_READING_BUFFER_LENGTH;
  uint8_t* buffer = malloc(length * sizeof(uint8_t));

  setControlBits(CONTROL_READ);
  for (uint16_t i = 0; i < length; i++) {
    q_setAddress(startAddress + i);
    //delayMicroseconds(DELAY_READING);
    buffer[i] = readDataPin();
  }
  setControlBits(CONTROL_DISABLE);

  return buffer;
}

/*
   Change the 16 bit address held by the shift register
*/
void ExternalEEPROM::setAddress(uint16_t address) {
  /*
  // Disattiva le uscite
  digitalWrite(G_PIN, HIGH);
  // Resetta (per sicurezza)
  pulse(SCL_PIN, LOW);
  */
  // Riempi lo shift register (al contrario)
  for (int i = ADDR_BITS - 1; i >= 0; i--) {
    // Poni il bit in uscita
    digitalWrite(SER_PIN, (address &  (1 << i)) != 0);
    // Fallo salvare allo shift register
    pulse(SCK_PIN, HIGH);
  }
  // Aggiorna i latch in uscita
  pulse(RCK_PIN, HIGH);
  /*
  // Abilita le uscite
  digitalWrite(G_PIN, LOW);
  */
}

void ExternalEEPROM::q_setAddress(uint16_t address) {
  for (int i = ADDR_BITS - 1; i >= 0; i--) {
    // Costruisci la maschera, controlla il bit e se è uno fai la OR con il bit giusto di PORTB altrimenti fai la AND con 0
    if((address &  (1 << i)) != 0)
      PORTB |= B00000100;   // SER_PIN è il D10 e quindi è il PB2
    else
      PORTB &= B11111011;
      
    // Fallo salvare allo shift register (flashando un HIGH sul pin SCK)
    PORTB |= B00001000;
    PORTB &= B11110111;
  }
  // Aggiorna i latch in uscita flashando un HIGH su RCK
  PORTB |= B00010000;
  PORTB &= B11101111;
}

/*
   Method to change the pinMode of the data pins according to what should arduino do (writing or reading from the EEPROM)
*/
void ExternalEEPROM::setMode(byte newMode) {
  if (mode == newMode)
    return;

  mode = newMode;
  if (mode == READING) {
    for (byte i = 0; i < DATA_BITS; i++) {
      pinMode(DATA_PINS[i], INPUT);
    }
  }
  else if (mode == WRITING) {
    for (byte i = 0; i < DATA_BITS; i++) {
      pinMode(DATA_PINS[i], OUTPUT);
    }
  }
}

/*
   Method to read the state of the data pins and convert it to a byte
*/
uint8_t ExternalEEPROM::readDataPin() {
  // Aggiorna il pinMode dei pin
  setMode(READING);
  uint8_t data = 0;
  // Leggi il dato
  for (byte i = 0; i < DATA_BITS; i++) {
    int pin = DATA_PINS[i];
    byte mask = 1 << i;

    if (digitalRead(pin))
      data += mask;
  }

  return data;
}

/*
   Method to put any 8 bit long data on data_pin (so the EEPROM can read it)
*/
void ExternalEEPROM::putOnDataPin(uint8_t data) {
  // Aggiorna il pinMode dei pin
  setMode(WRITING);
  for (byte i = 0; i < DATA_BITS; i++) {
    uint8_t mask = 1 << i;
    boolean state = (data & mask) != 0;

    digitalWrite(DATA_PINS[i], state);
  }
}

void ExternalEEPROM::q_putOnDataPin(uint8_t data) {
  setMode(WRITING);
  PORTD |= data << 2;
  uint8_t mask = data >> 6;
  PORTB |= mask;
}

/*
   Simple method to pulse HIGH (change rapidly from LOW to HIGH and again LOW) or viceversa
   one pin. Created so the code can result more clean.
*/
void ExternalEEPROM::pulse(byte pin, boolean type) {
  digitalWrite(pin, type);
  //delay(2000);
  digitalWrite(pin, !type);
  //delay(2000);
}

void ExternalEEPROM::setControlBits(uint8_t state) {
  digitalWrite(CE_PIN, (state & CE_MASK) != 0);
  digitalWrite(OE_PIN, (state & OE_MASK) != 0);
  digitalWrite(WE_PIN, (state & WE_MASK) != 0);
  delayMicroseconds(DELAY_PULSE);
}
