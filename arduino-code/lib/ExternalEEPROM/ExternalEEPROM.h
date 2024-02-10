#ifndef ExternalEEPROM_h
#define ExternalEEPROM_h

/* LIBRARIES */
#include "Arduino.h"

/* COSTANTS */
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

#define ADDR_BITS 16
#define ADDR_BYTES 2
#define DATA_BITS 8

/* MASKS */
#define PAGE_ID_SHIFT 6
#define PAGE_ID_MASK  0xFFC0
#define PAGE_POS_MASK 0x003F

#define SET_SER_PIN   B00000100   // SER_PIN is D10 so is PB2
#define RESET_SER_PIN B11111011
#define SET_SCK_PIN   B00001000   // SCK_PIN is D11 so is PB3
#define RESET_SCK_PIN B11110111
#define SET_RCK_PIN   B00010000   // RCK_PIN is D12 so is PB4
#define RESET_RCK_PIN B11101111

#define SET_CE_PIN    B00000001   // CE_PIN is A0 so is PC0
#define RESET_CE_PIN  B11111110
#define SET_WE_PIN    B00000100   // WE_PIN is A2 so is PC2
#define RESET_WE_PIN  B11111011

#define RESET_PORTD   B00000011
#define RESET_PORTB   B11111100

#define CE_MASK 0x01
#define OE_MASK 0x02
#define WE_MASK 0x04
        // They are all active-low!
#define CONTROL_DISABLE ( WE_MASK | OE_MASK |  CE_MASK)   // 111
#define CONTROL_ENABLE  ( WE_MASK | OE_MASK           )   // 110
#define CONTROL_READ    ( WE_MASK                     )   // 100
#define CONTROL_WRITE   (           OE_MASK           )   // 010
#define CONTROL_SDP     (           OE_MASK |  CE_MASK)   // 011


/* DELAYS */
#define DELAY_PULSE 2     // us
#define DELAY_WRITING 15  // ms
#define DELAY_READING 50  // us
#define DELAY_PAGE_WRITING 30  // us (each byte must be written within 150us of the previous byte)


/* LENGTHS */
#define MAX_READING_BUFFER_LENGTH 32
#define MAX_WRITING_PAGE_LENGTH 64


/* STATUS WORDS */
const byte WRITING = 'w';
const byte READING = 'r';


class ExternalEEPROM {
  public:
    // Costructor
    ExternalEEPROM();
    // Utility
    void write(uint16_t address, uint8_t data);
    void write(uint16_t address, int length, uint8_t* data);
    int pageWrite(uint16_t address, int length, uint8_t* data);
    uint8_t read(uint16_t address);
    uint8_t* read(uint16_t startAddress, uint8_t length);
    void enableDataProtection();
    void disableDataProtection();
    
    
  private:
    /* METHODS */
    void setMode(byte newMode);
    void setAddress(uint16_t address);
    void q_setAddress(uint16_t address);
    void pulse(byte pin, boolean type);
    uint8_t readDataPin();
    void putOnDataPin(uint8_t data);
    void q_putOnDataPin(uint8_t data);
    void setControlBits(uint8_t state);
    void q_pulse(byte pin, boolean type);
    /* VARIABLES */
    byte mode = WRITING;
};

#endif
