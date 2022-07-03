package app;

import com.fazecast.jSerialComm.SerialPortTimeoutException;
import cyberLib.arduino.ArduinoSerial;
import cyberLib.arduino.ArduinoSerial.BaudRates;
import cyberLib.io.Input;
import cyberLib.io.Printer;

public class App3 {

	public static void main(String[] args) throws SerialPortTimeoutException {
		ArduinoSerial serial = new ArduinoSerial("COM5", BaudRates.$74880);

		while (true) {
			int value = Input.askInt("Scegli numero");
			byte[] toByte = {0x10, (byte) (value & 0x000000ff), (byte) ((value & 0x0000ff00) >> 8) };
			Printer.printByteArray(toByte);

			serial.write(toByte);
		}
	}

}
