package eepromprogrammer;

import java.io.File;
import java.io.IOException;

import com.fazecast.jSerialComm.SerialPortInvalidPortException;

import cyberLib.io.Input;
import cyberLib.io.Menu;
import cyberLib.io.Printer;
import eepromprogrammer.eeproms.Eeprom;
import eepromprogrammer.eeproms.EepromManager;
import eepromprogrammer.eeproms.Eeprom_28C256;

public class App {

	public static final Option DEFAULT_OPTION = new Option("COM5", null, Option.ACTION_EVERY, Option.MODE_SINGLE);

	public static void main(String[] args) throws SerialPortInvalidPortException, IOException, InterruptedException {

		String[] prova = { "ciao", "-path=C:\\Users\\savol\\Desktop\\lettura.bin", "-cicic=asdsad", "", "-action=every" };

		Option option = Option.parseArguments(prova);
		option.overrideUndefined(DEFAULT_OPTION);
		System.out.println(option);
		System.out.println("\n\n");

		Eeprom eeprom = new Eeprom_28C256(option.getPort());
		EepromManager manager = new EepromManager(eeprom);

		if (option.isAction(Option.ACTION_READ)) {
			if (option.isMode(Option.MODE_SINGLE)) {
				while (true)
					readSingle(eeprom);
			} else if (option.isMode(Option.MODE_MULTIPLE)) {
				while (true)
					readMultiple(eeprom);
			}
		} else if (option.isAction(Option.ACTION_WRITE)) {
			// write
			if (option.isMode(Option.MODE_SINGLE)) {
				while (true)
					writeSingle(eeprom);
			}
		} else if (option.isAction(Option.ACTION_EVERY)) {
			Menu menu = new Menu("Main menu");
			menu.add("Exit");
			menu.add("Read single");
			menu.add("Read multiple");
			menu.add("Read all");
			menu.add("Write single");
			menu.add("Write multiple");
			menu.add("Write file");

			int selection = menu.select();
			while (selection != 0) {

				switch (selection) {
					case 1:
						readSingle(eeprom);
						break;
					case 2:
						readMultiple(eeprom);
						break;
	
					case 3: {
						// read all
						if (option.getPath() == null)
							option.setPath(Input.askString("Insert file name"));
	
						manager.readFile(new File(option.getPath()));
						break;
					}
	
					case 4:
						writeSingle(eeprom);
						break;
					case 5:
						writeMultiple(eeprom);
						break;
					case 6:
						if (option.getPath() == null)
							option.setPath(Input.askString("Insert file name"));
	
						manager.writeFile(new File(option.getPath()));
						break;
				}

				System.out.println("\n\n");
				selection = menu.select();
			}
			System.exit(0);
		} else
			// default
			throw new IllegalArgumentException("\"" + option.getAction() + "\" does not represent any action");
	}

	public static void readSingle(Eeprom eeprom) {
		int addr = (int) Input.askHexOrInt("Insert addr");
		System.out.printf("0x%04x: 0x%02x\n", addr, eeprom.readSingle(addr));
	}

	public static void writeSingle(Eeprom eeprom) {
		int addr = (int) Input.askHexOrInt("Insert addr");
		byte data = (byte) Input.askHexOrInt("Insert data");

		if (eeprom.writeSingle(addr, data))
			System.out.printf("Byte 0x%02x successfully written in address 0x%04x\n", data, addr);
		else
			System.out.printf("Byte 0x%02x was not written in address 0x%04x\n", data, addr);
	}

	public static void readMultiple(Eeprom eeprom) {
		int fromAddr = (int) Input.askHexOrInt("Insert from addr");
		int toAddr = (int) Input.askHexOrInt("Insert to addr");

		byte[] buff = eeprom.readMultiple(fromAddr, toAddr);
		Printer.printByteArray(buff);
	}

	public static void writeMultiple(Eeprom eeprom) {
		int fromAddr = (int) Input.askHexOrInt("Insert from addr");
		int length = (int) Input.askHexOrInt("Insert from length");

		byte[] buffer = new byte[length];
		for (int i = 0; i < buffer.length; i++) {
			buffer[i] = (byte) (i + 16);
		}

		System.out.println(eeprom.writeMultiple(fromAddr, buffer));
	}
}
