package cyberLib.arduino;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

public class SerialFileWriter extends Thread {
	
	/* TEXT */
	private static final String ERR_BUFFER_SIZE = "Buffer size cannot be less than 1";
	private static final String ERR_FILE = "File cannot be read";
	
	/* STATIC COSTANTS */
	public final static BaudRates BAUD_RATE = BaudRates.$74880;
	public final static int DEFAUL_BUFFER_SIZE = 32;
	
	/* COSTANTS */
	private final File file;
	private final ArduinoSerial serial;
	
	/* VARIABLES */
	private boolean writing;
	private int buffSize = DEFAUL_BUFFER_SIZE;
	
	public SerialFileWriter(String port, File file) throws IOException {
		this.file = file;
		if (!file.canRead())
			throw new IOException(ERR_FILE);
		serial = new ArduinoSerial(port, BAUD_RATE);
	}
	
	@Override
	public void start() {
		if(!writing) {
			writing = true;
			// Starting the thread
			super.start();
		}
	}
	
	public void kill() {
		writing = false;
	}
	
	@Override
	public void run() {
		int bytesSent = 0;
		try (BufferedInputStream reader = new BufferedInputStream(new FileInputStream(file), buffSize)) {
			while (writing) {
				// System.out.println("Waiting...");
				serial.waitFor(Commands.READY, this);
				// System.out.println("Arduino is ready");

				byte[] data = new byte[buffSize];
				int actualSize = reader.readNBytes(data, 0, data.length);

				if (actualSize == 0) {
					// The end-of-file has been reached:
					writing = false;
					System.out.println("File terminated");
					continue;
				}

				ArrayList<Byte> message = new ArrayList<>();
				message.add(Commands.NEW_PACKET); // Add command
				message.add((byte) (actualSize & 0x000000ff)); // Add the lower most byte of the size
				message.add((byte) ((actualSize & 0x0000ff00) >> 8)); // Add the upper most byte of the size
				for (int i = 0; i < actualSize; i++)
					message.add(data[i]);

				serial.write(message);
				
				bytesSent += actualSize;
			}
		} catch (FileNotFoundException e) {
			System.err.println("file error");
			e.printStackTrace();
			writing = false;
		} catch (IOException e) {
			System.err.println("io error");
			e.printStackTrace();
			writing = false;
		} catch (Exception e) {
			System.err.println("other errors");
			e.printStackTrace();
			writing = false;
		} finally {
			serial.write(Commands.END_OF_TRANSMISSION);
			System.out.println("Bytes sent: " + bytesSent);
		}
	}
	
	public void setBufferSize(int buffSize) {
		if(buffSize <= 0)
			throw new IllegalArgumentException(ERR_BUFFER_SIZE);
		this.buffSize = buffSize;
	}
	
	public boolean isWriting() {
		return writing;
	}
}
