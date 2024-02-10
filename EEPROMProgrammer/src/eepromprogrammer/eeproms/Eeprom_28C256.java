package eepromprogrammer.eeproms;

import com.fazecast.jSerialComm.SerialPortTimeoutException;

import cyberLib.arduino.ArduinoSerial;
import cyberLib.arduino.BaudRates;
import cyberLib.io.Printer;

import javax.sql.CommonDataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Eeprom_28C256 extends Eeprom {
	
	private static final String myName = "28C256";
	private static final int myMaxAddress = 0x8000;
	private static final BaudRates BAUD_RATE = BaudRates.$115200;
	
	private static final int MAX_READ_BUFFER_LENGTH = 30;
	
	private ArduinoSerial serial;

	public Eeprom_28C256(String port) throws SerialPortTimeoutException, IOException {
		super(myName, myMaxAddress);
		
		serial = new ArduinoSerial(port, BAUD_RATE);

		while(serial.available() < Commands.PACKET_LENGTH)
			;
		byte[] packet = serial.readBytes(Commands.PACKET_LENGTH);
		if(packet[0] != Commands.UTIL.READY)
			throw new IOException("Didn't receive UTIL_READY from arduino");
		serial.clear();
	}

	@Override
	public byte readSingle(long address) {
		byte[] message = Commands.makeCommand(Commands.READ.SINGLE, address);
		byte[] packet = serial.awaitResponse(message, Commands.PACKET_LENGTH);
		serial.clear();
		return packet[2];
	}

	@Override
	public byte[] readMultiple(long fromAddress, long length) {
		length = Math.min(length, MAX_READ_BUFFER_LENGTH);
		byte[] message = Commands.makeCommand(Commands.READ.MULTIPLE, fromAddress, length);
		byte[] packet = serial.awaitResponse(message, Commands.PACKET_LENGTH);
		serial.clear();

		if(packet[0] != Commands.UTIL.NEW_PACKET)
			serial.clear();

		int dataLength = packet[1];
		byte[] data = new byte[dataLength];
		System.arraycopy(packet, 2, data, 0, dataLength);
		return data;
	}

	@Override
	public boolean writeSingle(long address, byte data) {
		byte[] writeMessage = Commands.makeCommand(Commands.WRITE.SINGLE, address, data);
		byte[] packet = serial.awaitResponse(writeMessage, Commands.PACKET_LENGTH);
		serial.clear();

		if(packet[0] != Commands.UTIL.READY)
			return false;

		return checkSingle(address, data);
	}

	@Override
	public boolean writeMultiple(long address, byte[] data) {
		byte[] message = Commands.makeCommand(Commands.WRITE.MULTIPLE, data, address);
		byte[] packet = serial.awaitResponse(message, Commands.PACKET_LENGTH);
		
		return packet[0] == Commands.UTIL.READY;
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

	public void test(byte[] message) {
		byte[] response = serial.awaitResponse(message, Commands.PACKET_LENGTH);
		for(byte b : response) {
			System.out.printf("%c", b);
		}
		System.out.println();
	}
}
