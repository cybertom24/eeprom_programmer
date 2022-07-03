package app;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.JFileChooser;
import com.fazecast.jSerialComm.SerialPort;
import cyberLib.arduino.SerialFileWriter;
import cyberLib.io.Menu;

public class App2 {

	public static void main(String[] args) {
		try {
			start();
		} catch (Exception e) {
			e.printStackTrace();
			// Something...
		}
	}

	public static void start() throws IOException {
		// Get all ports available
		SerialPort[] ports = SerialPort.getCommPorts();
		// Create an arrayList of the ports' name
		ArrayList<String> portNames = new ArrayList<>();
		for (SerialPort sPort : ports)
			portNames.add(sPort.getDescriptivePortName());

		// Create the menu for selecting one
		Menu portsMenu = new Menu("Seleziona la porta");
		portsMenu.add(portNames);
		// Get the user's selection and retrieve it's "COM%" name
		String selectedPort = ports[portsMenu.select()].getSystemPortName();

		// Create the FileChooser object
		JFileChooser fChooser = new JFileChooser();
		fChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		// Ask the user about the new file and get their response
		int returnValue = fChooser.showOpenDialog(fChooser);
		// If the user cancelled the action:
		if (returnValue == JFileChooser.CANCEL_OPTION)
			return;
		// Else get the file chosen
		File fileToSend = fChooser.getSelectedFile();

		SerialFileWriter writer = new SerialFileWriter(selectedPort, fileToSend);
		writer.start();
	}

}
