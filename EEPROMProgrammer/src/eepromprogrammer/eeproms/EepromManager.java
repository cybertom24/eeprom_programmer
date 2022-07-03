package eepromprogrammer.eeproms;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import cyberLib.io.Printer;

public class EepromManager {
	
	
	private static final int READ_BUFFER_LENGTH = 28;
	private static final int WRITE_BUFFER_LENGTH = 28;
	
	private Eeprom eeprom;
	
	public EepromManager(Eeprom eeprom) {
		this.eeprom = eeprom;
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
