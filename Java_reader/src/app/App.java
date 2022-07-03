package app;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JFileChooser;
import cyberLib.arduino.*;
import cyberLib.io.Input;
import cyberLib.io.Menu;

import com.fazecast.jSerialComm.*;

public class App {
	//private static final String FILE_NAME = "content";
	//private static final String EXTENSION = "txt";
	//private static final String FOLDER_PATH = "C:\\Users\\savol\\Documents\\CyberIndustries\\Progetti\\EPROM reader-writer";
	
	private static final String PORTS_MENU_TITLE = "Seleziona la porta";
	
	public static void main(String[] args) {
		
		// Get all ports available
		SerialPort[] ports = SerialPort.getCommPorts(); 
		// Create an arrayList of the ports' name
		ArrayList<String> portNames = new ArrayList<>();
		for(SerialPort sPort : ports)
			portNames.add(sPort.getDescriptivePortName());
		
		// Create the menu for selecting one
		Menu portsMenu = new Menu(PORTS_MENU_TITLE);
		portsMenu.add(portNames);
		// Get the user's selection and retrieve it's "COM%" name 
		String selectedPort = ports[portsMenu.select()].getSystemPortName();
		
		// Create the FileChooser object
		JFileChooser fChooser = new JFileChooser();
		fChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		// Ask the user about the new file and get their response
		int returnValue = fChooser.showSaveDialog(fChooser);
		// If the user cancelled the action:
		if(returnValue == JFileChooser.CANCEL_OPTION)
			return;
		// Else get the file chosen
		File file = fChooser.getSelectedFile();
		
		SerialFileReader transcriptor;
		try {
			transcriptor = new SerialFileReader(selectedPort, file);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		while(true) {
			String command = Input.askString();
			
			if(command.equalsIgnoreCase(LineCommand.START)) {
				transcriptor.start();
			}
			else if(command.equalsIgnoreCase(LineCommand.STOP)) {
				transcriptor.stop();
			}
		}
	}
	
}
