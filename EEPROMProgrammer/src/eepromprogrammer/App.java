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

	public static final Option DEFAULT_OPTION = new Option("COM5", null, Option.ACTION_EVERY, true);

	public static void main(String[] args) throws SerialPortInvalidPortException, IOException {

		String[] prova = { "ciao", "-path=beemovie8.txt", "-cicic=asdsad", "", "-action=every" };

		Option option = Option.parseArguments(args);
		option.overrideUndefined(DEFAULT_OPTION);
		System.out.println(option + "\n");

		System.out.println("> Opening port...");
		Eeprom eeprom = new Eeprom_28C256(option.getPort());
		EepromManager manager = new EepromManager(eeprom, option.getPath(), option.getValidate());
		System.out.println("> Ready!");

		if (option.isAction(Option.ACTION_READ)) {
			if(option.getPath() == null)
				option.setPath(Input.askString("Insert path"));
			manager.readFile(new File(option.getPath()));
		} else if (option.isAction(Option.ACTION_WRITE)) {
			if(option.getPath() == null)
				option.setPath(Input.askString("Insert path"));
			manager.writeFile(new File(option.getPath()), 0, true);
		} else if (option.isAction(Option.ACTION_EVERY)) {
			manager.mainLoop();
		} else
			// default
			throw new IllegalArgumentException("\"" + option.getAction() + "\" does not represent any action");

		System.out.println("> Exiting program...");
		System.exit(0);
	}
}
