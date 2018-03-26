package ru.ifmo.rain.vaksman.implementor;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * This class provides {@link #write(String)} method to write string in Unicode encoding.
 *
 * @author Denis Vaksman
 */
public class UnicodeWriter extends OutputStreamWriter {

    /**
     * Creates an UnicodeWriter that uses the default character encoding.
     *
     * @param  out  An OutputStream
     */
    public UnicodeWriter(OutputStream out) {
        super(out);
    }

    /**
     * Writes given string in Unicode format.
     */
    public void write(String s) throws IOException {
        super.write(toUnicode(s));
    }

    /**
     * Converts given string to Unicode encoding.
     *
     * @param in string to convert
     * @return Converted string in Unicode encoding
     */
    private String toUnicode(String in) {
        StringBuilder b = new StringBuilder();

        for (char c : in.toCharArray()) {
            if (c >= 128)
                b.append("\\u").append(String.format("%04X", (int) c));
            else
                b.append(c);
        }

        return b.toString();
    }
}