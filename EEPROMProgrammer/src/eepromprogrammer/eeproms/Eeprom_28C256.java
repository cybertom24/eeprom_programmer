package eepromprogrammer.eeproms;

import com.fazecast.jSerialComm.SerialPortTimeoutException;

import cyberLib.arduino.ArduinoSerial;
import cyberLib.arduino.BaudRates;
import cyberLib.io.Printer;

public class Eeprom_28C256 extends Eeprom {
	
	private static final String myName = "28C256";
	private static final int myMaxAddress = 0x8000;
	private static final BaudRates BAUD_RATE = BaudRates.$74880;
	
	private static final int MAX_READ_BUFFER_LENGTH = 30;
	
	private ArduinoSerial serial;
	
	public Eeprom_28C256(String port) throws SerialPortTimeoutException {
		super(myName, myMaxAddress);
		
		serial = new ArduinoSerial(port, BAUD_RATE);
		serial.waitForSignal();
		serial.clear();
	}

	@Override
	public byte readSingle(long address) {
		byte[] message = Commands.makeCommand(Commands.READ.SINGLE, address);
		byte response = serial.awaitResponse(message)[0];
		return response;
	}

	@Override
	public byte[] readMultiple(long fromAddress, long length) {
		length = Math.min(length, MAX_READ_BUFFER_LENGTH);
		
		byte[] message = Commands.makeCommand(Commands.READ.MULTIPLE, fromAddress, length);
		byte[] response = serial.awaitResponse(message);
		return response;
	}

	@Override
	public boolean writeSingle(long address, byte data) {
		byte[] writeMessage = Commands.makeCommand(Commands.WRITE.SINGLE, address, data);
		serial.awaitResponse(writeMessage);
		return checkSingle(address, data);
	}

	@Override
	public boolean writeMultiple(long address, byte[] data) {
		byte[] message = Commands.makeCommand(Commands.WRITE.MULTIPLE, data, address);
		byte response = serial.awaitResponse(message)[0];
		
		return response == Commands.UTIL.READY;
	}

	/**
	 * Checks if the address contains the data expected
	 * @param address The address to check
	 * @param data The data expected
	 * @return true if the data read is the same as the data passed as parameter
	 */
	@Override
	public boolean checkSingle(long address, byte data) {
		return (data == readSingle(address));
	}

	/**
	 * Checks if it's possible to write on the EEPROM
	 * @return true if it's possible to write
	 */
	public boolean checkWrite() {
		int randomAddr = (int) (Math.random() * maxAddress);
		byte old = readSingle(randomAddr);
		boolean success = writeSingle(randomAddr, (byte) (old + 1));
		if(!success)
			return false;
		return writeSingle(randomAddr, old);
	}
}
