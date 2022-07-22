package eepromprogrammer.eeproms;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.sql.Array;
import java.text.FieldPosition;
import java.util.ArrayList;

import cyberLib.io.Input;
import cyberLib.io.Menu;
import cyberLib.io.Printer;

public class EepromManager {
	
	private static final int MAX_TRIES = 3;
	private static final double MAX_ERROR_ARRAY_LENGTH_PERCENTAGE = 0.05;
	private static final int VALIDATE_BUFFER_LENGTH = 32;
	private static final int READ_BUFFER_LENGTH = 28;
	private static final int WRITE_BUFFER_LENGTH = 28;
	private static final String MAIN_MENU_TITLE = "Main menu";
	private static final String[] MENU_ENTRIES = { "Exit", "Read single", "Read multiple", "Read all", "Write single",
			"Write multiple", "Write file", "Check writing", "Change path", "Toggle validation"};
	private final Menu mainMenu = new Menu(MAIN_MENU_TITLE);
	private Eeprom eeprom;
	private String path;
	private boolean validate;
	
	public EepromManager(Eeprom eeprom, String path, boolean validate) {
		this.eeprom = eeprom;
		for(String entry : MENU_ENTRIES)
			mainMenu.add(entry);
		this.path = path;
		this.validate = validate;
	}

	public boolean mainMenu() throws IOException {
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

				writeFile(new File(path), 0, validate);
				break;
			case 7:
				if(eeprom.checkWrite())
					System.out.println("Writing is enabled");
				else
					System.out.println("Writing is not enabled");
				break;
			case 8:
				path = Input.askString("Insert new path");
				break;
			case 9:
				validate = !validate;
				System.out.println("Validate set to " + validate);
				break;
		}

		return true;
	}

	public void mainLoop() throws IOException {
		while(mainMenu());
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
	public void readFile(File file) throws IOException {
		System.out.println("> Reading the eeprom memory and transcribing it to the file");
		
		
		try(BufferedOutputStream outStream = new BufferedOutputStream(new FileOutputStream(file))) {
			int exProgress = 0;
			long address = 0;
			while(address < eeprom.maxAddress) {
				long length = Math.min(READ_BUFFER_LENGTH, eeprom.maxAddress - address);
				byte[] buffer = eeprom.readMultiple(address, length);

				if(buffer.length <= 2)
					break;

				// Skip the header
				for(int i = 2; i < buffer.length; i++)
					outStream.write(buffer[i]);
				
				int progress = (int) (100*((double) address / eeprom.maxAddress));
				if(progress % 5 == 0 && exProgress != progress) {
					System.out.println("> " + progress + "%");
					exProgress = progress;
				}

				address += buffer.length - 2;
			}
			System.out.println("> Done");
		}
	}
	
	public void writeFile(File file, long startAddress, boolean validate) throws IOException {
		System.out.println("> Writing the file into eeprom memory");

		if(!eeprom.checkWrite())
			throw new IOException("Cannot write on EEPROM");
		
		try(InputStream inStream = new BufferedInputStream(new FileInputStream(file))) {
			int exProgress = 0;
			long address = 0;
			long maxAddress = Math.min(eeprom.maxAddress, file.length());
			while(address < maxAddress) {
				int length = (int) Math.min(WRITE_BUFFER_LENGTH, maxAddress - address);
				byte[] buffer = inStream.readNBytes(length);
				if(buffer.length < 1)
					break;

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
			return;
		}

		if(!validate)
			return;

		System.out.println("> Initiating validation process");
		ArrayList<Cell> cells2beRepaired = validate(file);

		if(cells2beRepaired.isEmpty()) {
			System.out.println("> Nothing has gone wrong. Writing process finished");
			return;
		}


		System.out.println("Found " + cells2beRepaired.size() + " with wrong data");
		System.out.println("> Initiating reparation process");
		repair(cells2beRepaired);
		System.out.println("> EEPROM written and ready to be used");
	}

	public ArrayList<Cell> validate(File expected) throws IOException {
		ArrayList<Cell> cells = new ArrayList<>();

		// Read the contents of the EEPROM
		File real = new File(".temp.bin");
		readFile(real);

		// Find errors
		try(InputStream inStreamExpected = new BufferedInputStream(new FileInputStream(expected));
			InputStream inStreamReal = new BufferedInputStream(new FileInputStream(real))) {
			long maxAddr = Math.min(expected.length(), real.length());
			long addr = 0;
			while(addr < maxAddr) {
				byte[] bufferExpected = inStreamExpected.readNBytes(VALIDATE_BUFFER_LENGTH);
				if(bufferExpected.length == 0)
					break;

				byte[] bufferReal = inStreamReal.readNBytes(bufferExpected.length);
				if(bufferReal.length != bufferExpected.length)
					throw new IOException("The lengths of the two buffer are different");

				for(int i = 0; i < bufferExpected.length; i++) {
					if (bufferExpected[i] != bufferReal[i])
						cells.add(new Cell(addr, bufferExpected[i]));
				}

				// If there are too many errors
				int maxErrors = (int) Math.ceil(MAX_ERROR_ARRAY_LENGTH_PERCENTAGE * expected.length());
				if(cells.size() > maxErrors)
					throw new IOException(String.format("Too many errors! Validation process halted at address: 0x%04x", addr));

				addr += bufferExpected.length;
			}
		}

		// Remove the temp file
		if(!real.delete())
			System.err.println("Cannot delete temp file");

		return cells;
	}

	public void repair(ArrayList<Cell> cells) throws IOException {
		for(Cell cell : cells) {
			// Try to repair the cell (max MAX_TRIES iterations)
			for(int i = 0; i < MAX_TRIES; i++) {
				if(eeprom.writeSingle(cell.address, cell.data))
					break;
			}
			// If the data has not been written
			if(eeprom.checkSingle(cell.address, cell.data)) {
				double progress = 100 * (cells.indexOf(cell) / (double) cells.size());
				throw new IOException("Repair of " + cell + " failed. Reparation process halted at " + progress + "%");
			}
		}
	}
}
