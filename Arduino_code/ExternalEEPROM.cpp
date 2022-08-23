#include "ExternalEEPROM.h"

/*
   Costructor. Initialize all of the pins and their status
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
  setControlBits(CONTROL_ENABLE);

  // Poni gli address in uscita
  q_setAddress(address);
  // Leggi il dato
  setControlBits(CONTROL_READ);
  uint8_t data = readDataPin();

  // Resetta
  setControlBits(CONTROL_DISABLE);
  return data;
}

/*
   Write on the EEPROM the byte passed at a specified address
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
*/
int ExternalEEPROM::pageWrite(uint16_t address, int length, uint8_t* data) {
  // Recupera l'id della pagina
  uint16_t pageId = address & PAGE_ID_MASK;
  // Recupera la posizione all'interno della pagina
  uint16_t pagePos = address & PAGE_POS_MASK;
  // Controlla che la lunghezza non ecceda la pagina
  if (length > MAX_WRITING_PAGE_LENGTH - pagePos)
    length = MAX_WRITING_PAGE_LENGTH - pagePos;
  
  setControlBits(CONTROL_ENABLE);
  uint16_t addr = (pageId | pagePos);
  for (uint16_t i = 0; i < length; i++) {
    q_setAddress(addr + i);
    q_putOnDataPin(data[i]);
    // WE is active-low
    PORTC &= (RESET_WE_PIN);
    delayMicroseconds(10);
    PORTC |= (SET_WE_PIN);
  }
  setControlBits(CONTROL_DISABLE);
  delay(DELAY_WRITING);
  
  return length;
}

/*
void ExternalEEPROM::write(uint16_t address, int length, uint8_t* data) {
  for(int i = 0; i < length; i++) {
    write(address + i, data[i]);
  }
}
*/

void ExternalEEPROM::write(uint16_t address, int length, uint8_t* data) {
  int i = 0;
  while(i < length) {
    // Scrivi e aggiona i dei byte che sono stati scritti (e quindi cambia pagina)
    i += pageWrite(address + i, length - i, &data[i]);
  }  
}

/*
   Remember to free the memory after usage
*/
uint8_t* ExternalEEPROM::read(uint16_t startAddress, uint8_t length) {
  if(length > MAX_READING_BUFFER_LENGTH)
    length = MAX_READING_BUFFER_LENGTH;
  uint8_t* buffer = (uint8_t*) malloc(length * sizeof(uint8_t));

  for (uint16_t i = 0; i < length; i++)
    buffer[i] = read(startAddress + i);

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
      PORTB |= SET_SER_PIN;   // SER_PIN è il D10 e quindi è il PB2
    else
      PORTB &= RESET_SER_PIN;
      
    // Fallo salvare allo shift register (flashando un HIGH sul pin SCK)
    PORTB |= SET_SCK_PIN;
    PORTB &= RESET_SCK_PIN;
  }
  // Aggiorna i latch in uscita flashando un HIGH su RCK
  PORTB |= SET_RCK_PIN;
  PORTB &= RESET_RCK_PIN;
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
  PORTD &= RESET_PORTD;
  PORTD |= data << 2;
  PORTB &= RESET_PORTB;
  PORTB |= data >> 6;
}

/*
   Simple method to pulse HIGH (change rapidly from LOW to HIGH and again LOW) or viceversa
   one pin. Created so the code can result more clean.
*/
void ExternalEEPROM::pulse(byte pin, boolean type) {
  digitalWrite(pin, type);
  delayMicroseconds(10);
  digitalWrite(pin, !type);
}

void ExternalEEPROM::setControlBits(uint8_t state) {
  digitalWrite(CE_PIN, (state & CE_MASK));
  digitalWrite(OE_PIN, (state & OE_MASK));
  digitalWrite(WE_PIN, (state & WE_MASK));
}
