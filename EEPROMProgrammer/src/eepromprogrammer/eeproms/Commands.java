package eepromprogrammer.eeproms;

public class Commands {
	
	public static byte[] makeCommand(byte command, long... data) {
		byte[] message;
		switch (command) {
			case READ.SINGLE -> {
				message = new byte[]{READ.SINGLE,
						getDownByte(data[0]),
						getUpByte(data[0])};
			}
			case READ.MULTIPLE -> {
				message = new byte[]{READ.MULTIPLE,
						getDownByte(data[0]),
						getUpByte(data[0]),
						getDownByte(data[1])};
			}
			case WRITE.SINGLE -> {
				message = new byte[]{WRITE.SINGLE,
						getDownByte(data[0]),
						getUpByte(data[0]),
						getDownByte(data[1])};
			}
			default -> message = null;
		}
		return message;
	}
	
	public static byte[] makeCommand(byte command, byte[] buffer, long... data) {
		switch(command) {
			case WRITE.MULTIPLE: {
				// 0x31 [1B length] [2B startAddress] [xB buffer]
				byte[] header = { WRITE.MULTIPLE,
						getDownByte(buffer.length),
						getDownByte(data[0]),
						getUpByte(data[0]) };
				
				byte[] message = new byte[header.length + buffer.length];
				System.arraycopy(header, 0, message, 0, header.length);
				System.arraycopy(buffer, 0, message, header.length, buffer.length);
				
				return message;
			}
			default:
				return null;
		}
	}
	
	private static byte getUpByte(long value) {
		return (byte) ((value & 0x0000ff00) >> 8);
	}
	
	private static byte getDownByte(long value) {
		return (byte) (value & 0x000000ff);
	}
	
	public class READ {
		public final static byte SINGLE = 0x10;
		public final static byte MULTIPLE = 0x11;
		public final static byte INIT = 0x12;
	}
	
	public class WRITE {
		public final static byte SINGLE = 0x20;
		public final static byte MULTIPLE = 0x21;
		public final static byte INIT = 0x22;
	}
	
	public class SDP {
		public final static byte ENABLE = 'e';
		public final static byte DISABLE = 'd';
	}
	
	public class UTIL {
		public final static byte READY = 0x30;
		public final static byte NEW_PACKET = 0x31;
		public final static byte EOF = 0x32;
		public final static byte EMPTY = '#';
	}
	
	public class ERROR {
		public final static byte WRONG_ARG = (byte) 0xF0;
		public final static byte OVERFLOW = (byte) 0xF1;
	}
}
