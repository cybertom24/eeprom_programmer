package eepromprogrammer;

import java.io.File;
import java.io.IOException;

import com.fazecast.jSerialComm.SerialPortInvalidPortException;

import cyberLib.io.Input;
import cyberLib.io.Menu;
import cyberLib.io.Printer;
import eepromprogrammer.eeproms.Eeprom;
import eepromprogrammer.eeproms.EepromManager;
import eepromprogrammer.eeproms.Eeprom_28C256;

public class App {

	public static final Option DEFAULT_OPTION = new Option("COM5", null, Option.ACTION_EVERY);

	public static void main(String[] args) throws SerialPortInvalidPortException, IOException {

		String[] prova = { "ciao", "-path=beemovie-letto.txt", "-cicic=asdsad", "", "-action=every" };

		Option option = Option.parseArguments(prova);
		option.overrideUndefined(DEFAULT_OPTION);
		System.out.println(option);
		System.out.println("\n\n");

		System.out.println("> Opening port...");
		Eeprom eeprom = new Eeprom_28C256(option.getPort());
		EepromManager manager = new EepromManager(eeprom);
		System.out.println("> Ready!");

		if (option.isAction(Option.ACTION_READ)) {
			if(option.getPath() == null)
				option.setPath(Input.askString("Insert path"));
			manager.readFile(new File(option.getPath()));
		} else if (option.isAction(Option.ACTION_WRITE)) {
			if(option.getPath() == null)
				option.setPath(Input.askString("Insert path"));
			manager.writeFile(new File(option.getPath()));
		} else if (option.isAction(Option.ACTION_EVERY)) {
			manager.mainLoop(option.getPath());
			System.exit(0);
		} else
			// default
			throw new IllegalArgumentException("\"" + option.getAction() + "\" does not represent any action");
	}
}
