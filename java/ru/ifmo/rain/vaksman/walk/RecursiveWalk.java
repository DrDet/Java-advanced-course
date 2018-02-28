package ru.ifmo.rain.vaksman.walk;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

public class RecursiveWalk {
    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.out.println("Incorrect input format.\nUsage:\nRecursiveWalk <input_file> <output_file>");
            return;
        }

        try (LineNumberReader reader = new LineNumberReader(new InputStreamReader(new FileInputStream(args[0]), "UTF-8"))) {
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(args[1]), "UTF-8")) {
                String curLine;
                byte[] buf = new byte[1024];
                FileVisitor fileVisitor = new FileVisitor(writer, buf);
                try {
                    while ((curLine = reader.readLine()) != null) {
                        try {
                            Files.walkFileTree(Paths.get(curLine), fileVisitor);
                        } catch (InvalidPathException e) {
                            fileVisitor.writeData(0, curLine);
                            System.out.println("Invalid path name of the following file: " + curLine + "\nReason: " + e.getMessage());
                        } catch (SecurityException e) {
                            fileVisitor.writeData(0, curLine);
                            System.out.println("Unable to access the following file: " + curLine + "\nReason: " + e.getMessage());
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Read error in the input file:\n" + e.getMessage());
                }
            } catch (FileNotFoundException e) {
                System.out.println("Unable to open the output file:\n" + e.getMessage());
            }
        } catch (FileNotFoundException e) {
            System.out.println("Unable to open the input file:\n" + e.getMessage());
        } catch (UnsupportedEncodingException e) {
            System.out.println("Unsupported encoding:\n" + e.getMessage());
        } catch (IOException e) {
            System.out.println("Close error:\n" + e.getMessage());
        } catch (RuntimeException e) {
            System.out.println("Runtime exception:\n" + e.getMessage());
        }
    }
}
