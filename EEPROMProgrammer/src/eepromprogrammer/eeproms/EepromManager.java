package eepromprogrammer.eeproms;

import cyberLib.io.CLI;
import cyberLib.io.Input;
import cyberLib.io.Menu;
import cyberLib.io.Printer;

import java.io.*;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class EepromManager {

    private static final int MAX_TRIES = 3;
    private static final double MAX_ERROR_ARRAY_LENGTH_PERCENTAGE = 0.05;
    private static final int VALIDATE_BUFFER_LENGTH = 32;
    private static final int READ_BUFFER_LENGTH = 28;
    private static final int WRITE_BUFFER_LENGTH = 28;
    private static final String MAIN_MENU_TITLE = "Main menu";
    private static final String[] MENU_ENTRIES = {"Exit", "Read single", "Read multiple", "Read all", "Write single",
            "Write multiple", "Write file", "Check writing", "Change path", "Toggle validation"};
    private final Menu mainMenu = new Menu(MAIN_MENU_TITLE);
    private Eeprom eeprom;
    private String path;
    private String scriptPath = null;
    private boolean exit = false;
    private boolean validate;
    private CLI cli;

    public EepromManager(Eeprom eeprom, String path, boolean validate) throws IOException {
        this.eeprom = eeprom;
        for (String entry : MENU_ENTRIES)
            mainMenu.add(entry);
        this.path = getAbsoulutePath(path);
        this.validate = validate;
    }

    public EepromManager(Eeprom eeprom, String path, boolean validate, String scriptPath) throws IOException {
        this.eeprom = eeprom;
        for (String entry : MENU_ENTRIES)
            mainMenu.add(entry);
        this.path = getAbsoulutePath(path);
        this.validate = validate;
        this.scriptPath = getAbsoulutePath(scriptPath);
    }

    public boolean mainMenu() throws IOException {
        System.out.println("\n-----------------------------------------");

        String command = Input.askString("> Insert command");
        return manageCommand(command);
    }

    public void executeScript() {
        File scriptFile = new File(scriptPath);
        try (Scanner fileScanner = new Scanner(new FileInputStream(scriptFile))) {
            boolean runAgain = true;
            while(runAgain) {
                System.out.println("\n-----------------------------------------");
                String command = fileScanner.nextLine();
                System.out.println("> Command: " + command);
                runAgain = manageCommand(command);
            }
        } catch (FileNotFoundException e) {
            System.out.println("> Error: failed to load script file");
        } catch (NoSuchElementException e) {
        }

        System.out.println("> Script ended");
        scriptPath = null;
    }

    public void mainLoop() throws IOException {
        if(scriptPath != null) {
            System.out.println("> Mode: script " + scriptPath);
            executeScript();
        }
        else {
            System.out.println("> Mode: CLI");
            while (mainMenu()) ;
        }

        if(exit)
            return;

        mainLoop();
    }

    private boolean manageCommand(String command) {
        String[] arguments = command.split(" ");
        String action = arguments[0];

        if (isIgnoreCase(action, "write", "w")) {
            System.out.println("Action: write");
            if(arguments.length < 3) {
                System.out.println("Not enough arguments");
                return true;
            }

            // Same data, range of address (check if the arguments list contains any ":" o "to"
            if (Arrays.stream(arguments).anyMatch(s -> isIgnoreCase(s, ":", "to"))) {
                byte data = 0;
                long firstAddress = 0, lastAddress = 0;
                try {
                    data = Integer.decode(arguments[1]).byteValue();

                    if (isIgnoreCase(arguments[2], "in")) {
                        firstAddress = Long.decode(arguments[3]);

                        if (arguments[5].equals("."))
                            lastAddress = eeprom.maxAddress;
                        else
                            lastAddress = Long.decode(arguments[5]);
                        // The argument with index 4 is ":" or "to"
                    } else {
                        firstAddress = Long.decode(arguments[2]);
                        if (arguments[4].equals("."))
                            lastAddress = eeprom.maxAddress;
                        else
                            lastAddress = Long.decode(arguments[4]);
                        // The argument with index 3 is ":" or "to"
                    }
                } catch (IndexOutOfBoundsException e) {
                    System.out.println("Not enough arguments");
                    return true;
                } catch (NumberFormatException e) {
                    System.out.println("Number format not recognized");
                    return true;
                }

                System.out.printf("data: 0x%02x%n", data);
                System.out.printf("first address: 0x%04x%n", firstAddress);
                System.out.printf("last address: 0x%04x%n", lastAddress);
                return true;
            }

            // Start address, multiple data (check if arguments contain "from")
            if (Arrays.stream(arguments).anyMatch(s -> isIgnoreCase(s, "from"))) {
                // Get the list of data to be written
                int separatorIndex = indexOf(arguments, "from");
                String[] dataStringList = Arrays.copyOfRange(arguments, 1, separatorIndex);
                byte[] data = new byte[dataStringList.length];
                long firstAddress = 0;

                try {
                    for(int i = 0; i < dataStringList.length; i++) {
                        data[i] = Byte.decode(dataStringList[i]);
                    }

                    firstAddress = Long.decode(arguments[arguments.length - 1]);
                } catch (IndexOutOfBoundsException e) {
                    System.out.println("Not enough arguments");
                    return true;
                } catch (NumberFormatException e) {
                    System.out.println("Number format not recognized");
                    return true;
                }

                System.out.println("data:");
                for(byte b : data) {
                    System.out.printf("0x%02x%n", b);
                }
                System.out.printf("first address: 0x%04x%n", firstAddress);
                return true;
            }

            // Same data, list of address ("in" is in third place)
            if(isIgnoreCase(arguments[2], "in")) {
                String[] addressesStringList = Arrays.copyOfRange(arguments, 3, arguments.length);
                long[] addresses = new long[addressesStringList.length];
                byte data = 0;

                try {
                    for(int i = 0; i < addressesStringList.length; i++) {
                        addresses[i] = Long.decode(addressesStringList[i]);
                    }

                    data = Byte.decode(arguments[1]);
                } catch (IndexOutOfBoundsException e) {
                    System.out.println("Not enough arguments");
                    return true;
                } catch (NumberFormatException e) {
                    System.out.println("Number format not recognized");
                    return true;
                }

                System.out.printf("data: 0x%02x%n", data);
                System.out.println("addresses:");
                for(long l : addresses) {
                    System.out.printf("0x%04x%n", l);
                }
                return true;
            }

            // List of data and list of addresses
            int separatorIndex = indexOf(arguments, "in");
            String[] dataStringList = Arrays.copyOfRange(arguments, 1, separatorIndex);
            byte[] data = new byte[dataStringList.length];
            String[] addressesStringList = Arrays.copyOfRange(arguments, separatorIndex + 1, arguments.length);
            long[] addresses = new long[addressesStringList.length];

            if(data.length != addresses.length) {
                System.out.println("Error: number of data does not match number of addresses");
                return true;
            }

            try {
                for(int i = 0; i < addressesStringList.length; i++) {
                    addresses[i] = Long.decode(addressesStringList[i]);
                }

                for(int i = 0; i < dataStringList.length; i++) {
                    data[i] = Byte.decode(dataStringList[i]);
                }
            } catch (IndexOutOfBoundsException e) {
                System.out.println("Not enough arguments");
                return true;
            } catch (NumberFormatException e) {
                System.out.println("Number format not recognized");
                return true;
            }

            System.out.println("data:");
            for(byte b : data) {
                System.out.printf("0x%02x%n", b);
            }
            System.out.println("addresses:");
            for(long l : addresses) {
                System.out.printf("0x%04x%n", l);
            }
            return true;
        }
        else if (isIgnoreCase(action, "read", "r")) {
            // Multiple read
            if (arguments.length > 2) {
                System.out.println("Action: read multiple");

                long firstAddress = 0, lastAddress = 0;
                try {
                    if (isIgnoreCase(arguments[1], "from")) {
                        firstAddress = Long.decode(arguments[2]);

                        if (isIgnoreCase(arguments[3], "to", ":"))
                            lastAddress = Long.decode(arguments[4]);
                        else
                            lastAddress = Long.decode(arguments[3]);
                    } else {
                        firstAddress = Long.decode(arguments[1]);

                        if (isIgnoreCase(arguments[2], "to", ":"))
                            lastAddress = Long.decode(arguments[3]);
                        else
                            lastAddress = Long.decode(arguments[2]);
                    }
                } catch (IndexOutOfBoundsException e) {
                    System.out.println("Not enough arguments");
                    return true;
                } catch (NumberFormatException e) {
                    System.out.println("Number format not recognized");
                    return true;
                }

                System.out.printf("first address: 0x%04x%n", firstAddress);
                System.out.printf("last address: 0x%04x%n", lastAddress);
                return true;
            }

            // Single read
            System.out.println("Action: read");
            long address = 0;
            try {
                address = Long.decode(arguments[1]);
            } catch (IndexOutOfBoundsException e) {
                System.out.println("Not enough arguments");
                return true;
            } catch (NumberFormatException e) {
                System.out.println("Number format not recognized");
                return true;
            }

            System.out.printf("address: 0x%04x%n", address);
        }
        else if (isIgnoreCase(action, "file", "f")) {
            if (arguments.length > 1) {
                path = getAbsoulutePath(arguments[1]);
            }

            System.out.println("file set to: " + path);
        }
        else if (isIgnoreCase(action, "upload", "u")) {
            if (arguments.length > 1) {
                String newPath = getAbsoulutePath(arguments[1]);
                if(newPath == null) {
                    System.out.println("Error: failed to parse file path");
                    return true;
                }

                System.out.println("Uploading file: " + newPath);
            }

            System.out.println("Uploading file: " + path);
        }
        else if (isIgnoreCase(action, "download", "d")) {
            if (arguments.length > 1) {
                String newPath = getAbsoulutePath(arguments[1]);
                if(newPath == null) {
                    System.out.println("Error: failed to parse file path");
                    return true;
                }

                System.out.println("Downloading memory content on file: " + newPath);
            }

            System.out.println("Downloading memory content on file: " + path);
        }
        else if (isIgnoreCase(action, "validate", "v")) {
            if (arguments.length > 1) {
                if (isIgnoreCase(arguments[1], "on", "yes", "y", "true", "t", "1"))
                    validate = true;
                else if (isIgnoreCase(arguments[1], "off", "no", "n", "false", "f", "0"))
                    validate = false;
                else
                    System.out.println("Validate option not recognized");
            }

            System.out.println("Validate is set to: " + validate);
        }
        else if (isIgnoreCase(action, "script", "s")) {
            if (arguments.length == 1) {
                System.out.println("Error: Not enough arguments");
                return true;
            }

            scriptPath = getAbsoulutePath(arguments[1]);

            if(scriptPath == null) {
                System.out.println("Error: failed to parse file path");
                return true;
            }
            // Stop the loop
            return false;
        }
        else if (isIgnoreCase(action, "quit", "q")) {
            exit = true;
            return false;
        }
        else
            System.out.println("Command not recognized");

        return true;
    }

    public void readSingle() {
        int addr = (int) Input.askHexOrInt("Insert addr");
        System.out.printf("0x%04x: 0x%02x\n", addr, eeprom.readSingle(addr));
    }

    public void writeSingle() {
        int addr = (int) Input.askHexOrInt("Insert addr");
        byte data = (byte) Input.askHexOrInt("Insert data");

        if (eeprom.writeSingle(addr, data))
            System.out.printf("Byte 0x%02x successfully written in address 0x%04x\n", data, addr);
        else
            System.out.printf("Byte 0x%02x was not written in address 0x%04x\n", data, addr);
    }

    public void readMultiple() {
        int fromAddr = (int) Input.askHexOrInt("Insert from addr");
        int toAddr = (int) Input.askHexOrInt("Insert to addr");

        byte[] buff = eeprom.readMultiple(fromAddr, toAddr);
        Printer.printByteArray(buff);
    }

    public void writeMultiple() {
        int fromAddr = (int) Input.askHexOrInt("Insert from addr");
        int length = (int) Input.askHexOrInt("Insert from length");

        byte[] buffer = new byte[length];
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = (byte) (i + 16);
        }

        System.out.println(eeprom.writeMultiple(fromAddr, buffer));
    }

    /**
     * Metodo per leggere la memoria della eeprom e trascriverla su file
     *
     * @param file
     */
    public void readFile(File file) throws IOException {
        System.out.println("> Reading the eeprom memory and transcribing it to the file");


        try (BufferedOutputStream outStream = new BufferedOutputStream(new FileOutputStream(file))) {
            int exProgress = 0;
            long address = 0;
            while (address < eeprom.maxAddress) {
                long length = Math.min(READ_BUFFER_LENGTH, eeprom.maxAddress - address);
                byte[] buffer = eeprom.readMultiple(address, length);

                if (buffer.length <= 2)
                    break;

                // Skip the header
                for (int i = 2; i < buffer.length; i++)
                    outStream.write(buffer[i]);

                int progress = (int) (100 * ((double) address / eeprom.maxAddress));
                if (progress % 5 == 0 && exProgress != progress) {
                    System.out.println("> " + progress + "%");
                    exProgress = progress;
                }

                address += buffer.length - 2;
            }
            System.out.println("> Done");
        }
    }

    public void writeFile(File file, long startAddress, boolean validate) throws IOException {
        System.out.println("> Writing the file into eeprom memory");

        if (!eeprom.checkWrite())
            throw new IOException("Cannot write on EEPROM");

        try (InputStream inStream = new BufferedInputStream(new FileInputStream(file))) {
            int exProgress = 0;
            long address = 0;
            long maxAddress = Math.min(eeprom.maxAddress, file.length());
            while (address < maxAddress) {
                int length = (int) Math.min(WRITE_BUFFER_LENGTH, maxAddress - address);
                byte[] buffer = inStream.readNBytes(length);
                if (buffer.length < 1)
                    break;

                eeprom.writeMultiple(address, buffer);

                int progress = (int) (100 * ((double) address / eeprom.maxAddress));
                if (progress % 5 == 0 && exProgress != progress) {
                    System.out.println("> " + progress + "%");
                    exProgress = progress;
                }

                address += buffer.length;
            }
            System.out.println("> Done");

        } catch (FileNotFoundException e) {
            System.err.println("File " + file.getName() + " has not been found");
            System.err.println("The writing action will not complete");
            return;
        }

        if (!validate)
            return;

        System.out.println("> Initiating validation process");

        ArrayList<Cell> cells2beRepaired;
        try {
            cells2beRepaired = validate(file);
        } catch (IOException e) {
            System.out.println("> " + e.getMessage());
            System.out.println("> Writing failed. Consider trying again");
            return;
        }

        if (cells2beRepaired.isEmpty()) {
            System.out.println("> Nothing has gone wrong. Writing process finished");
            return;
        }

        System.out.println("Found " + cells2beRepaired.size() + " with wrong data");
        System.out.println("> Initiating reparation process");
        try {
            repair(cells2beRepaired);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            System.out.println("> Writing failed. Consider trying again");
            return;
        }

        System.out.println("> EEPROM written and ready to be used");
    }

    public ArrayList<Cell> validate(File expected) throws IOException {
        ArrayList<Cell> cells = new ArrayList<>();

        // Read the contents of the EEPROM
        File real = new File(".temp.bin");
        readFile(real);

        // Find errors
        try (InputStream inStreamExpected = new BufferedInputStream(new FileInputStream(expected));
             InputStream inStreamReal = new BufferedInputStream(new FileInputStream(real))) {
            long maxAddr = Math.min(expected.length(), real.length());
            long addr = 0;
            while (addr < maxAddr) {
                byte[] bufferExpected = inStreamExpected.readNBytes(VALIDATE_BUFFER_LENGTH);
                if (bufferExpected.length == 0)
                    break;

                byte[] bufferReal = inStreamReal.readNBytes(bufferExpected.length);
                if (bufferReal.length != bufferExpected.length)
                    throw new IOException("The lengths of the two buffer are different");

                for (int i = 0; i < bufferExpected.length; i++) {
                    if (bufferExpected[i] != bufferReal[i])
                        cells.add(new Cell(addr, bufferExpected[i]));
                }

                // If there are too many errors
                int maxErrors = (int) Math.ceil(MAX_ERROR_ARRAY_LENGTH_PERCENTAGE * expected.length());
                if (cells.size() > maxErrors)
                    throw new IOException(String.format("Too many errors! Validation process halted at address: 0x%04x", addr));

                addr += bufferExpected.length;
            }
        }

        // Remove the temp file
        if (!real.delete())
            System.err.println("Cannot delete temp file");

        return cells;
    }

    public void repair(ArrayList<Cell> cells) throws IOException {
        for (Cell cell : cells) {
            // Try to repair the cell (max MAX_TRIES iterations)
            for (int i = 0; i < MAX_TRIES; i++) {
                if (eeprom.writeSingle(cell.address, cell.data))
                    break;
            }
            // If the data has not been written
            if (eeprom.checkSingle(cell.address, cell.data)) {
                double progress = 100 * (cells.indexOf(cell) / (double) cells.size());
                throw new IOException("Repair of " + cell + " failed. Reparation process halted at " + progress + "%");
            }
        }
    }

    public String getAbsoulutePath(String path) {
        File currentDirectory = new File("");
        String currentPath = currentDirectory.getAbsolutePath();
        File yourFile = new File(currentPath + "\\" + path);

        try {
            return yourFile.getCanonicalPath();
        } catch (IOException e) {
            return null;
        }
    }

    private static boolean isIgnoreCase(String string, String... aliases) {
        for (String alias : aliases) {
            if (string.equalsIgnoreCase(alias))
                return true;
        }
        return false;
    }

    private static int indexOf(String[] arr, String val) {
        for (int i = 0; i < arr.length; i++) {
			if(isIgnoreCase(arr[i], val))
                return i;
        }
        return -1;
    }
}
