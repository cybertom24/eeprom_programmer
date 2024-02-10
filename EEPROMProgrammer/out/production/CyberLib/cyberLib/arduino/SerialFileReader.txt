package cyberLib.arduino;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import com.fazecast.jSerialComm.*;

public class SerialFileReader {
	
	/* COSTANTS */
	public final static String DEFAULT_EXTENSION = ".txt";
	public final static BaudRates BAUD_RATE = BaudRates.$74880;
	
	/* VARIABLES */
	private final File file;
	private ArduinoSerial serial;
	private boolean reading = false;
	
	public SerialFileReader(String port, String fileName, String extension, String folderPath) throws IOException, SerialPortInvalidPortException {
		String path = new StringBuilder(folderPath)
				.append(File.pathSeparator)
				.append(fileName)
				.append(".")
				.append(extension)
				.toString();
		file = new File(path);
		
		serial = new ArduinoSerial(port, BAUD_RATE);
	}
	
	public SerialFileReader(String port, File file) throws IOException, SerialPortInvalidPortException {
		this.file = file;
		serial = new ArduinoSerial(port, BAUD_RATE);
	}

	public void start() {		
		reading = true;
		new Thread(new Runnable() {
			@Override
			public void run() {
				BufferedOutputStream buffOutStream;
				// On start:
				try {
					buffOutStream = new BufferedOutputStream(new FileOutputStream(file));
				} catch (IOException e) {
					e.printStackTrace();
					buffOutStream = null;
					reading = false;
				}
				serial.write(Commands.START);
				
				// Loop:
				while(reading && buffOutStream != null) {
					if(serial.available()) {
						byte[] data = serial.readBytes();
						try {
							for(byte b : data)
								buffOutStream.write(b);
						} catch (IOException e) {
							e.printStackTrace();
							reading = false;
						}
					}
				}
				
				// On finish:
				try {
					buffOutStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
	
	public void stop() {
		serial.write(Commands.STOP);
		reading = false;
	}
	
	public ArduinoSerial getSerial() {
		return serial;
	}
}
