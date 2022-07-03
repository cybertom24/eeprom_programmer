/*
   Based on the 28C256 EEPROM
   Datasheet at: ...

   EPROM pins:
   A0 ... A11: address
   IO0 ... IO7: output

   Designing choiches:
   Arduino NANO (328P) has 12 usable digital IO pins (D2 ... D13) and 8 analog IO pins (A0 ... A7)
   To make the things simple the 8 analog pins are assigned to the output pins of the EEPROM
   with A0(arduino) to IO0 and A7(arduino) to IO7. The rest of the pins will be in order
   The first 11 digital pins are wired to the 11 address pins of the EEPROM (A11 to A14 of the EEPROM will be hardwired to GND) with D2 to A0(eeprom) and D12 to A10(eeprom).
   The rest of the pins will be in order
   D13 is connected to CHIP ENABLE (pin 20)
   D0 and D1 remain dedicated to the Serial

   A6 and A7 can be only analog inputs
*/

#define COMMAND_START 0x69
#define COMMAND_STOP 0x96

#define s_TITLE "--- Arduino NANO based 28C256 EEPROM writer ---"
#define s_INIT1 "[!] Initializing pins..."
#define s_INIT2 "[!] Setting up pins' statuses..."
#define s_START "[!] Begin writing..."
#define s_END "[!] Writing finished"
#define s_HEX_PREFIX "0x"

#define BAUD_RATE 74880
#define ADDRESS_BITS 11
#define DATA_BITS 8
#define DELAY 15
#define READ_UNTIL pow(2, ADDRESS_BITS)
#define NEWLINE_EVERY 16

const int ADDRESS_PINS[] = { 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
const int DATA_PINS[] = { A0, A1, A2, A3, A4, A5, A6, A7};
const int WE_PIN = 13;

int address = 0;
byte count = 0;

boolean going = false;

void setup() {
  Serial.begin(BAUD_RATE);
  Serial.println(s_TITLE);
  Serial.println(s_INIT1);

  for (int i = 0; i < ADDRESS_BITS; i++)
    pinMode(ADDRESS_PINS[i], OUTPUT);
  for (int i = 0; i < DATA_BITS; i++)
    pinMode(DATA_PINS[i], OUTPUT);
  pinMode(WE_PIN, OUTPUT);

  Serial.println(s_INIT2);
  for (int i = 0; i < ADDRESS_BITS; i++)
    digitalWrite(ADDRESS_PINS[i], LOW);
  digitalWrite(WE_PIN, HIGH);

  Serial.println(s_START);
  Serial.print(s_HEX_PREFIX);
  Serial.print(address, HEX);
  Serial.print(": ");

  // Cycle:
  setupAddress(1);
  digitalWrite(WE_PIN, LOW);
  delayMicroseconds(1);
  writeData(0x69);
  delayMicroseconds(1);
  digitalWrite(WE_PIN, HIGH);
  wait();
}

void loop() {
}

void setupAddress(int addr) {
  for (byte i = 0; i < ADDRESS_BITS; i++) {
    int pin = ADDRESS_PINS[i];
    int mask = 1 << i;
    boolean status = (addr & mask) != 0;

    digitalWrite(pin, status);
  }
}

byte writeData(byte data) {
  for (byte i = 0; i < DATA_BITS; i++) {
    int pin = DATA_PINS[i];
    int mask = 1 << i;
    boolean state = (data & mask) != 0;
    digitalWrite(pin, state);
  }

  return data;
}

void wait() {
  delay(DELAY);
}

void stop() {
  Serial.print("\n");
  Serial.println(s_END);
  while (1);
}
