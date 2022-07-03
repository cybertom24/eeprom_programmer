package eepromprogrammer.eeproms;

public abstract class Eeprom {
	
	public final String name;
	public final long maxAddress;
	
	public Eeprom(String name, int maxAddress) {
		this.name = name;
		this.maxAddress = maxAddress;
	}
	
	public abstract byte readSingle(long address);
	
	public abstract byte[] readMultiple(long fromAddress, long length);
	
	public abstract boolean writeSingle(long address, byte data);
	
	public abstract boolean writeMultiple(long address, byte[] data);
	
	public abstract boolean checkSingle(long address, byte data);
}
