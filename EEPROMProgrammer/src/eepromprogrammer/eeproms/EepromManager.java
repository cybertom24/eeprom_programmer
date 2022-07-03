package eepromprogrammer.eeproms;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import cyberLib.io.Input;
import cyberLib.io.Menu;
import cyberLib.io.Printer;

public class EepromManager {
	
	
	private static final int READ_BUFFER_LENGTH = 28;
	private static final int WRITE_BUFFER_LENGTH = 28;
	private static final String MAIN_MENU_TITLE = "Main menu";
	private static final String[] MENU_ENTRIES = { "Exit", "Read single", "Read multiple", "Read all", "Write single",
			"Write multiple", "Write file", "Check writing"};
	private final Menu mainMenu = new Menu(MAIN_MENU_TITLE);
	private Eeprom eeprom;
	
	public EepromManager(Eeprom eeprom) {
		this.eeprom = eeprom;
		for(String entry : MENU_ENTRIES)
			mainMenu.add(entry);
	}

	public boolean mainMenu(String path) {
		System.out.println("\n\n");
		int selection = mainMenu.select();
		if(selection == 0)
			return false;

		switch (selection) {
			case 1:
				readSingle();
				break;
			case 2:
				readMultiple();
				break;

			case 3: {
				// read all
				if (path == null)
					path = Input.askString("Insert file name");

				readFile(new File(path));
				break;
			}

			case 4:
				writeSingle();
				break;
			case 5:
				writeMultiple();
				break;
			case 6:
				if (path == null)
					path = Input.askString("Insert file name");

				writeFile(new File(path));
				break;
			case 7:
				if(eeprom.checkWrite())
					System.out.println("Writing is enabled");
				else
					System.out.println("Writing is not enabled");
				break;
		}

		return true;
	}

	public void mainLoop(String path) {
		while(mainMenu(path));
	}

	public void readSingle() {
		int addr = (int) Input.askHexOrInt("Insert addr");
		System.out.printf("0x%04x: 0x%02x\n", addr, eeprom.readSingle(addr));
	}

	public void writeSingle() {
		int addr = (int) Input.askHexOrInt("Insert addr");
		byte data = (byte) Input.askHexOrInt("Insert data");

		if (eeprom.writeSingle(addr, data))
			System.out.printf("Byte 0x%02x successfully written in address 0x%04x\n", data, addr);
		else
			System.out.printf("Byte 0x%02x was not written in address 0x%04x\n", data, addr);
	}

	public void readMultiple() {
		int fromAddr = (int) Input.askHexOrInt("Insert from addr");
		int toAddr = (int) Input.askHexOrInt("Insert to addr");

		byte[] buff = eeprom.readMultiple(fromAddr, toAddr);
		Printer.printByteArray(buff);
	}

	public void writeMultiple() {
		int fromAddr = (int) Input.askHexOrInt("Insert from addr");
		int length = (int) Input.askHexOrInt("Insert from length");

		byte[] buffer = new byte[length];
		for (int i = 0; i < buffer.length; i++) {
			buffer[i] = (byte) (i + 16);
		}

		System.out.println(eeprom.writeMultiple(fromAddr, buffer));
	}
	/**
	 * Metodo per leggere la memoria della eeprom e trascriverla su file
	 * @param file
	 */
	public void readFile(File file) {
		System.out.println("> Reading the eeprom memory and trascribing it to the file");
		
		
		try(BufferedOutputStream outStream = new BufferedOutputStream(new FileOutputStream(file))) {
			
			int exProgress = 0;
			for(long address = 0; address < eeprom.maxAddress; address += READ_BUFFER_LENGTH) {
				if(address >= eeprom.maxAddress)
					address = eeprom.maxAddress - 1;
				long length = Math.min(READ_BUFFER_LENGTH, eeprom.maxAddress - address);
				byte[] buffer = eeprom.readMultiple(address, length);
				
				// Skip the header
				for(int i = 2; i < buffer.length; i++)
					outStream.write(buffer[i]);
				
				int progress = (int) (100*((double) address / eeprom.maxAddress));
				if(progress % 5 == 0 && exProgress != progress) {
					System.out.println("> " + progress + "%");
					exProgress = progress;
				}
			}
			System.out.println("> Done");
			
		} catch (FileNotFoundException e) {
			System.err.println("File " + file.getName() + " has not been found");
			System.err.println("The reading action will not complete");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeFile(File file) {
		System.out.println("> Writing the file into eeprom memory");
		
		try(InputStream inStream = new BufferedInputStream(new FileInputStream(file))) {
			int exProgress = 0;
			long address = 0;
			while(address < eeprom.maxAddress) {
				byte[] buffer = inStream.readNBytes(WRITE_BUFFER_LENGTH);
				if(buffer.length < 1)
					break;
				
				//Printer.printByteArray(buffer);
				
				eeprom.writeMultiple(address, buffer);
				
				int progress = (int) (100*((double) address / eeprom.maxAddress));
				if(progress % 5 == 0 && exProgress != progress) {
					System.out.println("> " + progress + "%");
					exProgress = progress;
				}
				
				address += buffer.length;
			}
			System.out.println("> Done");
			
		} catch (FileNotFoundException e) {
			System.err.println("File " + file.getName() + " has not been found");
			System.err.println("The writing action will not complete");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
		
}
