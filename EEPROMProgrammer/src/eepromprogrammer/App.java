package eepromprogrammer;

import com.fazecast.jSerialComm.SerialPortInvalidPortException;
import cyberLib.io.Input;
import eepromprogrammer.eeproms.Eeprom;
import eepromprogrammer.eeproms.EepromManager;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.io.IOException;

public class App {

	static class Options {

		@Option(name = "-v", aliases = "--validate",
				usage = "Checks if the file has been successfully written")
		public boolean validate = false;

		@Option(name = "-s", aliases = "--script",
				usage = "File to be used as script", metaVar = "script")
		public String script = null;

		@Option(name = "-f", aliases = "--file",
				usage = "File to be read or written", metaVar = "file")
		public String file = null;

		@Option(name = "-p", aliases = "--port",
				usage = "Port to be used", metaVar = "port")
		public String port = "COM11";

		@Option(name = "-r", aliases = "--read",
				usage = "Read the content of the EEPROM and write it to the file", metaVar = "read")
		public boolean read = false;

		@Option(name = "-w", aliases = "--write",
				usage = "Read the content of the file and write it to the EEPROM", metaVar = "write")
		public boolean write = false;
	}

	public static void main(String[] args) throws SerialPortInvalidPortException, IOException {

		//args = new String[]{ "-f", "./ciao/prova.txt", "-r", "-w"};

		Options options = new Options();
		CmdLineParser parser = new CmdLineParser(options);

		try {
			parser.parseArgument(args);
		} catch(CmdLineException e) {
			e.printStackTrace();
			System.out.print("eeprom");
			parser.printSingleLineUsage(System.out);
			System.out.println();
			parser.printUsage(System.out);
			System.exit(1);
		}

		System.out.println("----------- EEPROM Programmer -----------");
		System.out.println("Options:");
		System.out.println(printOptions(options));
		System.out.println("-----------------------------------------");
		System.out.println("> Opening port...");
		Eeprom eeprom = null; //new Eeprom_28C256(options.port);
		EepromManager manager = null;
		if(options.script == null)
			manager = new EepromManager(eeprom, options.file, options.validate);
		else
			manager = new EepromManager(eeprom, options.file, options.validate, options.script);
		System.out.println("> Ready!");

		if (options.read && !options.write) {
			if(options.file == null)
				options.file = Input.askString("Insert path");
			manager.readFile(new File(options.file));
		} else if (!options.read && options.write) {
			if(options.file == null)
				options.file = Input.askString("Insert path");
			manager.writeFile(new File(options.file), 0, true);
		} else {
			manager.mainLoop();
		}

		System.out.println("> Exiting program...");
		System.exit(0);
	}

	private static String printOptions(Options options) {
		StringBuilder builder = new StringBuilder("port = ");

		if (options.port == null)
			builder.append("undefined");
		else
			builder.append(options.port);

		builder.append("\nfile = ");
		if (options.file == null)
			builder.append("undefined");
		else
			builder.append(options.file);

		builder.append("\nscript = ");
		if (options.script == null)
			builder.append("undefined");
		else
			builder.append(options.script);


		if(options.read && !options.write)
			builder.append("\nread");
		else if(!options.read && options.write)
			builder.append("\nwrite");
		else
			builder.append("\ncli");

		builder.append("\nvalidate = ");
		builder.append(options.validate);

		return builder.toString();
	}
}
