package eepromprogrammer.eeproms;

public class Commands {

	public static final int PACKET_LENGTH = 32;

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

		if (message == null)
			return null;

		return fill(message);
	}
	
	public static byte[] makeCommand(byte command, byte[] buffer, long... data) {
		byte message[] = null;
		switch (command) {
			case WRITE.MULTIPLE -> {
				// 0x21 [1B length] [2B startAddress] [xB buffer]
				byte[] header = {WRITE.MULTIPLE,
						getDownByte(buffer.length),
						getDownByte(data[0]),
						getUpByte(data[0])};

				message = new byte[header.length + buffer.length];
				System.arraycopy(header, 0, message, 0, header.length);
				System.arraycopy(buffer, 0, message, header.length, buffer.length);
			}
			default -> {
				message = null;
			}
		}

		if (message == null)
			return null;

		return fill(message);
	}

	private static byte[] fill(byte[] message) {
		if(message.length == PACKET_LENGTH)
			return message;

		byte[] packet = new byte[PACKET_LENGTH];
		for (int i = 0; i < PACKET_LENGTH; i++) {
			if (i < message.length)
				packet[i] = message[i];
			else
				packet[i] = 0;
		}
		return packet;
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
