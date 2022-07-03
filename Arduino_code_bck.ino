/*
   --- Arduino based EEPROM Programmer ---
   Build around the 28C256 EEPROM

   Features:
   16 bit address
   8 bit data I/O (read and write)
   3 pin for controlling the EEPROM (chip enable, write enable and output enable)
*/


/* COSTANTS */
#define NEWLINE_EVERY 16

/* COMMANDS */                            // Structure
const byte CONSOLE_READ_SINGLE = 'r';     // r 32       (read address 32)
const byte CONSOLE_READ_MULTIPLE = 'R';   // R 32;100   (read from address 32 to address 100)
const byte CONSOLE_WRITE_SINGLE = 'w';    // w 32;100   (write in address 32 the number 100)


/* PIN */
// For controlling the two Si-Po
#define SER_PIN 10    // Serial out
#define SCK_PIN 11    // Serial clock
#define RCK_PIN 12    // Register clock (for updating the output)
#define G_PIN 13      // Output enable (active low)
#define SCL_PIN A5    // Clear (active low)
// For reading data
const byte DATA_PINS[] = {2, 3, 4, 5, 6, 7, 8, 9};
// For controlling the EEPROM
#define CE_PIN A0
#define OE_PIN A1
#define WE_PIN A2

void setup() {
  Serial.begin(74880);

  /* Pin mode initialization */
  pinMode(SER_PIN, OUTPUT);
  pinMode(SCK_PIN, OUTPUT);
  pinMode(RCK_PIN, OUTPUT);
  pinMode(G_PIN, OUTPUT);
  pinMode(SCL_PIN, OUTPUT);

  pinMode(CE_PIN, OUTPUT);
  pinMode(WE_PIN, OUTPUT);
  pinMode(OE_PIN, OUTPUT);

  setMode(READING);


  /* Pin status initialization */
  digitalWrite(SER_PIN, LOW);
  digitalWrite(SCK_PIN, LOW);
  digitalWrite(RCK_PIN, LOW);
  digitalWrite(G_PIN, LOW);
  digitalWrite(SCL_PIN, HIGH);

  digitalWrite(CE_PIN, LOW);
  digitalWrite(OE_PIN, HIGH);
  digitalWrite(WE_PIN, HIGH);
}

void loop() {
  if (Serial.available()) {
    byte command = Serial.read();

    Serial.println("\t------------");
    
    switch (command) {
      case CONSOLE_READ_SINGLE: {
          Serial.read();
          int addr = Serial.parseInt();
          Serial.print("> Reading from addr 0x");
          Serial.println(addr, HEX);

          byte data = read(addr);
          Serial.print("Data: 0x");
          Serial.println(data, HEX);
          break;
        }
      case CONSOLE_READ_MULTIPLE: {
          Serial.read();
          String num = Serial.readStringUntil(';');
          uint16_t fromAddr = num.toInt();
          num = Serial.readString();
          uint16_t toAddr = num.toInt();

          // Se l'address di arrivo è prima dell'address di partenza
          if(toAddr < fromAddr) {
            // Scambiali
            uint16_t temp = toAddr;
            toAddr = fromAddr;
            fromAddr = toAddr;
          }
          
          Serial.print("> Reading from addr 0x");
          Serial.print(fromAddr, HEX);
          Serial.print(" to addr 0x");
          Serial.println(toAddr, HEX);

          byte count = 0;
          Serial.print("0x");
          Serial.print(fromAddr, HEX);
          Serial.print(": ");
          for(uint16_t i = fromAddr; i <= toAddr; i++) {
            Serial.print(read(i), HEX);
            Serial.print(" ");
            
            count++;
            if(count >= NEWLINE_EVERY && i != toAddr) {  
              count = 0;
              Serial.print("\n0x");
              Serial.print(i + 1, HEX);
              Serial.print(": ");
            }
          }
          Serial.println();
          break;
        }
      case CONSOLE_WRITE_SINGLE: {
          Serial.read();
          String num = Serial.readStringUntil(';');
          int addr = num.toInt();
          num = Serial.readString();
          byte data = num.toInt();

          Serial.print("> Writing to addr 0x");
          Serial.print(addr, HEX);
          Serial.print(" byte: 0x");
          Serial.println(data, HEX);

          write(addr, data);
          break;
        }
      default: {
          Serial.println("default");
          break;
        }
    }
  }
}

/*
 * Change the 16 bit address held by the shift register
 */
void setAddress(uint16_t address) {
  // Disattiva le uscite
  digitalWrite(G_PIN, HIGH);
  // Resetta (per sicurezza)
  pulse(SCL_PIN, LOW);
  // Riempi lo shift register (al contrario)
  for (int i = ADDR_BITS - 1; i >= 0; i--) {
    // Ricava il bit
    uint16_t mask = 1 << i;
    boolean bit = (address & mask) != 0;
    // Poni il bit in uscita
    digitalWrite(SER_PIN, bit);
    // Fallo salvare allo shift register
    pulse(SCK_PIN, HIGH);
  }
  // Aggiorna i latch in uscita
  pulse(RCK_PIN, HIGH);
  // Abilita le uscite
  digitalWrite(G_PIN, LOW);
}

/*
 * Simple method to pulse HIGH (change rapidly from LOW to HIGH and again LOW) or viceversa
 * one pin. Created so the code can result more clean.
 */
void pulse(byte pin, boolean type) {
  digitalWrite(pin, type);
  delayMicroseconds(DELAY_PULSE);
  digitalWrite(pin, !type);
  delayMicroseconds(DELAY_PULSE);
}

/*
 * Method to change the pinMode of the data pins according to what should arduino do (writing or reading from the EEPROM)
 */
void setMode(byte newMode) {
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
 * Method to read data contained in a particular address
 */
byte read(uint16_t address) {
  digitalWrite(OE_PIN, LOW);
  digitalWrite(WE_PIN, HIGH);
  
  // Poni gli address in uscita
  setAddress(address);
  delayMicroseconds(DELAY_READING);
  // Leggi il dato
  byte data = readDataPin();

  digitalWrite(OE_PIN, HIGH);
  return data;
}

/*
 * Write on the EEPROM the byte passed at a specified address
 */
void write(uint16_t addr, uint8_t data) {
  digitalWrite(OE_PIN, HIGH);
  digitalWrite(WE_PIN, HIGH);

  setAddress(addr);
  putOnDataPin(data);
  pulse(WE_PIN, LOW);
  delay(15);
}

/*
 * Method to read the state of the data pins and convert it to a byte
 */
byte readDataPin() {
  // Aggiorna il pinMode dei pin
  setMode(READING);
  byte data = 0;
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
 * Method to put any 8 bit long data on data_pin (so the EEPROM can read it)
 */
void putOnDataPin(uint8_t data) {
  // Aggiorna il pinMode dei pin
  setMode(WRITING);
  for(byte i = 0; i < DATA_BITS; i++) {
    uint8_t mask = 1 << i;
    boolean state = (data & mask) != 0;

    digitalWrite(DATA_PINS[i], state);
  }
}
