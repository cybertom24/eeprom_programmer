package eepromprogrammer;

import com.fazecast.jSerialComm.SerialPortTimeoutException;
import eepromprogrammer.eeproms.Cell;
import org.junit.Test;

import java.io.*;

import static org.junit.Assert.assertEquals;

public class Tests {
    //@Test
    public void testFileLength() throws SerialPortTimeoutException {
        File file1 = new File("beemovie.txt");
        File file2 = new File("beemovie-letto.txt");
        System.out.println(file1.length());
        System.out.println(file2.length());
        System.out.println("Scelto: " + Math.min(file1.length(), file2.length()));
    }

    @Test
    public void testReadingFromFile() throws IOException {
        File read = new File("beemovie.txt");
        File write = new File("write.txt");

        try(InputStream inStream = new BufferedInputStream(new FileInputStream(read));
            OutputStream outStream = new BufferedOutputStream(new FileOutputStream(write))) {

            long addr = 0;
            while (addr < read.length()) {
                byte[] buffer = inStream.readNBytes(28);
                outStream.write(buffer);

                addr += buffer.length;
            }
            assertEquals(addr, read.length());
        }
        assertEquals(write.length(), read.length());

        long errors = 0;
        try(InputStream inStreamExpected = new BufferedInputStream(new FileInputStream(read));
            InputStream inStreamReal = new BufferedInputStream(new FileInputStream(write))) {
            long maxAddr = Math.min(read.length(), write.length());
            long addr = 0;
            while(addr < maxAddr) {
                byte[] bufferExpected = inStreamExpected.readNBytes(32);
                if(bufferExpected.length == 0)
                    break;

                byte[] bufferReal = inStreamReal.readNBytes(bufferExpected.length);
                if(bufferReal.length != bufferExpected.length)
                    throw new IOException("The lengths of the two buffer are different");

                for(int i = 0; i < bufferExpected.length; i++) {
                    if (bufferExpected[i] != bufferReal[i])
                        errors++;
                }

                addr += bufferExpected.length;
            }
        }
        assertEquals(errors, 0);
    }
}
