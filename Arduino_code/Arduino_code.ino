/*
   --- Arduino based EEPROM Programmer ---
   Build around the 28C256 EEPROM

   Features:
   16 bit address
   8 bit data I/O (read and write)
   3 pin for controlling the EEPROM (chip enable, write enable and output enable)
*/

/* LIBRARIES */
#include "ExternalEEPROM.h"

/* COSTANTS */
#define NEWLINE_EVERY 16
#define BAUD_RATE 74880
#define MAX_DATA_BUFFER_LENGTH 28

/* COMMANDS */                        // Structure
#define CONSOLE_READ_SINGLE       'r'     // r 32       (read address 32)
#define CONSOLE_READ_MULTIPLE     'R'     // R 32;100   (read from address 32 to address 100)
#define CONSOLE_WRITE_SINGLE      'w'     // w 32;100   (write in address 32 the number 100)
#define CONSOLE_TEST              't'
#define SCRIPT_READ_SINGLE        0x10    // 0x10 [2 byte address]                (read from addr F3A0: 0x10 0xA0 0xF3)
#define SCRIPT_READ_MULTIPLE      0x11    // 0x10 [2 byte fromAddress] [1 byte length]               (read from addr C3A0 to addr E001: 0x11 0xA0 0xC3 distance in hex)
#define SCRIPT_WRITE_SINGLE       0x20    // 0x20 [2 byte address] [1 byte data]  (write to addr F3A0 the data 1C: 0x10 0xA0 0xF3 0x1C) 
#define SCRIPT_WRITE_MULTIPLE     0x21    // 0x21 [1 byte dataLength] [2 byte startAddress] [dataLength bytes data] 
//#define SCRIPT_INIT_WRITE         0x22    // 0x22 [2 byte startAddress]           (start the write routine specifing the starting address)
#define SCRIPT_UTIL_READY         0x30    // 0x30                                 (tells the other device it's ready to receive new packets)
#define SCRIPT_UTIL_NEW_PACKET    0x31    // 0x31 [2 byte dataLength] [dataLength bytes data]
#define SCRIPT_UTIL_EOF           0x32    // 0x32                                 (signifies the end of file or transmission)     
#define SCRIPT_ERROR_WRONG_ARG    0xF0    // 0xF0                                 (indicates that one argument was not correct)
#define SCRIPT_ERROR_OVERFLOW     0xF1    // 0xF1
#define UTIL_EMPTY                '#'

/* PROTOTYPES */
uint16_t getTwoBytesValue();

ExternalEEPROM eeprom = ExternalEEPROM();

void setup() {
  Serial.begin(BAUD_RATE);
}

void loop() {  
  if (Serial.available()) {
    byte command = Serial.read();
    switch (command) {
      case CONSOLE_READ_SINGLE: {
          Serial.read();
          int addr = Serial.parseInt();
          Serial.print("> Reading from addr 0x");
          Serial.println(addr, HEX);

          byte data = eeprom.read(addr);
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
          if (toAddr < fromAddr) {
            // Scambiali
            uint16_t temp = toAddr;
            toAddr = fromAddr;
            fromAddr = toAddr;
          }

          Serial.print("> Reading from addr 0x");
          Serial.print(fromAddr, HEX);
          Serial.print(" to addr 0x");
          Serial.println(toAddr, HEX);

          Serial.print("0x");
          Serial.print(fromAddr, HEX);
          Serial.print(": ");

          while (fromAddr <= toAddr) {
            uint16_t length = NEWLINE_EVERY;
            if (toAddr - fromAddr < length)
              length = toAddr - fromAddr;

            uint8_t* buffer = eeprom.read(fromAddr, length);
            for (int i = 0; i < length; i++) {
              Serial.print(buffer[i], HEX);
              Serial.print(" ");
            }
            free(buffer);

            fromAddr += length;
            Serial.print("\n0x");
            Serial.print(fromAddr, HEX);
            Serial.print(": ");
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

          eeprom.write(addr, data);
          break;
        }
      case CONSOLE_TEST: {
          Serial.println("> Test");
          uint8_t data[64] = { 13, 2, 3, 4, 5, 6, 7, 8,
                               9, 10, 11, 12, 13, 14, 15, 16,
                               17, 18, 19, 20, 21, 22, 23, 24,
                               25, 26, 27, 28, 29, 30, 31, 32,
                               1, 2, 3, 4, 5, 6, 7, 8,
                               9, 10, 11, 12, 13, 14, 15, 16,
                               17, 18, 19, 20, 21, 22, 23, 24,
                               25, 26, 27, 28, 29, 30, 31, 32
                             };
          eeprom.write(150, 64, data);
          Serial.println("finish");
          break;
        }
      case SCRIPT_READ_SINGLE: {
          // The next two bytes are the address
          uint16_t address = getTwoBytesValue();
          Serial.write(eeprom.read(address));
          break;
        }
      case SCRIPT_WRITE_SINGLE: {
          uint8_t buff[3];
          Serial.readBytes(buff, 3);
          uint16_t address = * (uint16_t*) buff;
          uint8_t data = buff[2];

          eeprom.write(address, data);
          Serial.write(SCRIPT_UTIL_READY);
          break;
        }
      case SCRIPT_READ_MULTIPLE: {
          uint8_t buff[3];
          Serial.readBytes(buff, 3);
          uint16_t fromAddr = * (uint16_t *) buff;
          uint8_t length = buff[2];
          if(length > MAX_DATA_BUFFER_LENGTH)
            length = MAX_DATA_BUFFER_LENGTH;

          uint8_t* buffer = eeprom.read(fromAddr, length);
          uint8_t header[] = { SCRIPT_UTIL_NEW_PACKET, length };
          Serial.write(header, 2);
          Serial.write(buffer, length);
          free(buffer);
          break;
        }
      case SCRIPT_WRITE_MULTIPLE: {
          uint8_t buff[3];
          Serial.readBytes(buff, 3);
          uint8_t length = buff[0];
          uint16_t startAddress = * (uint16_t *) &buff[1];

          uint8_t *dataBuff = (uint8_t*) malloc(length * sizeof(uint8_t));
          Serial.readBytes(dataBuff, length);

          eeprom.write(startAddress, length, dataBuff);
          free(dataBuff);
          Serial.write(SCRIPT_UTIL_READY);
          break;
        }
        /*
      case SCRIPT_INIT_WRITE: {
          uint16_t address = getTwoBytesValue();

          while (true) {
            // Invia il comando di "pronto"
            Serial.write(SCRIPT_UTIL_READY);
            // Aspetta il comando
            while (Serial.available() == 0)
              ;

            if (Serial.read() == SCRIPT_UTIL_EOF)
              break;

            // Recupera la lunghezza del pacchetto
            uint16_t length = getTwoBytesValue();

            if (length <= 0) {
              Serial.write(SCRIPT_ERROR_WRONG_ARG);
              break;
            }

            // Alloca abbastanza memoria
            uint8_t *dataBuff = (uint8_t*) malloc(length * sizeof(uint8_t));
            // Leggi i dati passati
            Serial.readBytes(dataBuff, length);

            // Scrivi nella eeprom
            eeprom.write(address, length, dataBuff);

            // Libera la memoria
            free(dataBuff);
            // Aggiorna l'address (controllando che non vada in overflow)
            if (address + length < address) {
              Serial.write(SCRIPT_ERROR_OVERFLOW);
              break;
            }
            address += length;
          }
          break;
        }*/
      default: {
          Serial.println(UTIL_EMPTY);
          break;
        }
    }
  }
}

uint16_t getTwoBytesValue() {
  uint8_t buff[2];
  Serial.readBytes(buff, 2);
  return * (uint16_t*) buff;
}
