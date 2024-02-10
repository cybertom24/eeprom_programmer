/*
   --- Arduino based EEPROM Programmer ---
   Build around the 28C256 EEPROM

   Features:
   16 bit address
   8 bit data I/O (read and write)
   3 pin for controlling the EEPROM (chip enable, write enable and output enable)

   Serial protocol:
   Send always 32 bytes of data. Fill unused space with zeros
*/

/* LIBRARIES */
#include "ExternalEEPROM.h"

/* COSTANTS */
#define NEWLINE_EVERY 16
#define BAUD_RATE 115200
#define MAX_DATA_BUFFER_LENGTH 28
#define PACKET_LENGTH 32

/* COMMANDS */                    // Structure
#define CONSOLE_READ_SINGLE 'r'   // r 32       (read address 32)
#define CONSOLE_READ_MULTIPLE 'R' // R 32;100   (read from address 32 to address 100)
#define CONSOLE_WRITE_SINGLE 'w'  // w 32;100   (write in address 32 the number 100)
#define CONSOLE_TEST1 't'
#define CONSOLE_TEST2 'T'
#define SCRIPT_READ_SINGLE 0x10    // 0x10 [2 byte address]                (read from addr F3A0: 0x10 0xA0 0xF3)
#define SCRIPT_READ_MULTIPLE 0x11  // 0x11 [2 byte fromAddress] [1 byte length]               (read from addr C3A0 to addr E001: 0x11 0xA0 0xC3 distance in hex)
#define SCRIPT_WRITE_SINGLE 0x20   // 0x20 [2 byte address] [1 byte data]  (write to addr F3A0 the data 1C: 0x10 0xA0 0xF3 0x1C)
#define SCRIPT_WRITE_MULTIPLE 0x21 // 0x21 [1 byte dataLength] [2 byte startAddress] [dataLength bytes data]
// #define SCRIPT_INIT_WRITE         0x22    // 0x22 [2 byte startAddress]           (start the write routine specifing the starting address)
#define SCRIPT_UTIL_READY 0x30      // 0x30                                 (tells the other device it's ready to receive new packets)
#define SCRIPT_UTIL_NEW_PACKET 0x31 // 0x31 [1 byte dataLength] [dataLength bytes data]
#define SCRIPT_UTIL_EOF 0x32        // 0x32                                 (signifies the end of file or transmission)
#define SCRIPT_ERROR_WRONG_ARG 0xF0 // 0xF0                                 (indicates that one argument was not correct)
#define SCRIPT_ERROR_OVERFLOW 0xF1  // 0xF1
#define UTIL_EMPTY '#'

/* PROTOTYPES */
void SerialWrite(uint8_t *message, size_t length);
void SerialWrite(uint8_t single);

ExternalEEPROM eeprom = ExternalEEPROM();

void setup()
{
    Serial.begin(BAUD_RATE);

    SerialWrite(SCRIPT_UTIL_READY);
}

void loop()
{
    if (Serial.available() >= PACKET_LENGTH)
    {
        uint8_t packet[PACKET_LENGTH];
        Serial.readBytes(packet, PACKET_LENGTH);
        switch (packet[0])
        {
        case CONSOLE_READ_SINGLE:
        {
            Serial.read();
            int addr = Serial.parseInt();
            Serial.print("> Reading from addr 0x");
            Serial.println(addr, HEX);

            byte data = eeprom.read(addr);
            Serial.print("Data: 0x");
            Serial.println(data, HEX);
            break;
        }
        case CONSOLE_READ_MULTIPLE:
        {
            Serial.read();
            String num = Serial.readStringUntil(';');
            uint16_t fromAddr = num.toInt();
            num = Serial.readString();
            uint16_t toAddr = num.toInt();

            // Se l'address di arrivo è prima dell'address di partenza
            if (toAddr < fromAddr)
            {
                // Scambiali
                uint16_t temp = toAddr;
                toAddr = fromAddr;
                fromAddr = temp;
            }

            Serial.print("> Reading from addr 0x");
            Serial.print(fromAddr, HEX);
            Serial.print(" to addr 0x");
            Serial.println(toAddr, HEX);

            Serial.print("0x");
            Serial.print(fromAddr, HEX);
            Serial.print(": ");

            while (fromAddr < toAddr)
            {
                uint16_t length = NEWLINE_EVERY;
                if (toAddr - fromAddr < length)
                    length = toAddr - fromAddr;

                uint8_t *buffer = eeprom.read(fromAddr, length);
                for (int i = 0; i < length; i++)
                {
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
        case CONSOLE_WRITE_SINGLE:
        {
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
        case CONSOLE_TEST1:
        {
            Serial.println("> Test 1");
            uint8_t data[28] = {1, 2, 3, 4, 5, 6, 7,
                                8, 9, 10, 11, 12, 13, 14,
                                15, 16, 17, 18, 19, 20, 21,
                                22, 23, 24, 25, 26, 27, 28};
            eeprom.write(0, 28, data);
            Serial.println("finish");
            break;
        }
        case CONSOLE_TEST2:
        {
            Serial.println("> Test 2");
            uint8_t data[28] = {28, 27, 26, 25, 24, 23, 22,
                                21, 20, 19, 18, 17, 16, 15,
                                14, 13, 12, 11, 10, 9, 8,
                                7, 6, 5, 4, 3, 2, 1};
            eeprom.write(0, 28, data);
            Serial.println("finish");
            break;
        }
        case SCRIPT_READ_SINGLE:
        {
            // The next two bytes are the address
            uint16_t address = * (uint16_t *) &packet[1];
            uint8_t reading[] = {SCRIPT_UTIL_NEW_PACKET, 0x01, eeprom.read(address)};
            SerialWrite(reading, 3);
            break;
        }
        case SCRIPT_WRITE_SINGLE:
        {
            uint16_t address = *(uint16_t *) &packet[1];
            uint8_t data = packet[3];

            eeprom.write(address, data);
            SerialWrite(SCRIPT_UTIL_READY);
            break;
        }
        case SCRIPT_READ_MULTIPLE:
        {
            uint16_t fromAddr = *(uint16_t *) &packet[1];
            uint8_t length = packet[3];
            if (length > MAX_DATA_BUFFER_LENGTH)
                length = MAX_DATA_BUFFER_LENGTH;

            uint8_t *data = eeprom.read(fromAddr, length);
            uint8_t message[2 + length];
            message[0] = SCRIPT_UTIL_NEW_PACKET;
            message[1] = length;
            for (size_t i = 0; i < length; i++)
                message[i + 2] = data[i];
            SerialWrite(message, length + 2);
            free(data);
            break;
        }
        case SCRIPT_WRITE_MULTIPLE:
        {
            uint8_t length = packet[1];
            uint16_t startAddress = *(uint16_t *) &packet[2];

            uint8_t dataBuff[length];
            for (size_t i = 0; i < length; i++)
                dataBuff[i] = packet[i + 4];

            eeprom.write(startAddress, length, dataBuff);
            SerialWrite(SCRIPT_UTIL_READY);
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
        default:
        {
            Serial.println(UTIL_EMPTY);
            break;
        }
        }
    }
}

void SerialWrite(uint8_t *message, size_t length) {
    if (length < PACKET_LENGTH) {
        uint8_t packet[PACKET_LENGTH] = { 0 };
        memcpy(packet, message, length);
        Serial.write(packet, PACKET_LENGTH);
    } 
    else {
        Serial.write(message, PACKET_LENGTH);
    }
}

void SerialWrite(uint8_t single) {
    uint8_t message[1] = { single };
    SerialWrite(message, 1);
}