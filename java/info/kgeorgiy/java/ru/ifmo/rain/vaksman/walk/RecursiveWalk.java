package ru.ifmo.rain.vaksman.walk;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

public class RecursiveWalk {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Incorrect input format.\nUsage:\nRecursiveWalk <input_file> <output_file>");
        } else {
            try (LineNumberReader reader = new LineNumberReader(new InputStreamReader(new FileInputStream(args[0]), "UTF-8"))) {
                try (Writer writer = new OutputStreamWriter(new FileOutputStream(args[1]), "UTF-8")) {
                    String curLine;
                    byte[] buf = new byte[1024];
                    try {
                        while ((curLine = reader.readLine()) != null) {
                            FileVisitor fileVisitor = new FileVisitor(writer, buf);
                            try {
                                Files.walkFileTree(Paths.get(curLine), fileVisitor);
                            } catch (InvalidPathException e) {
                                fileVisitor.writeData(0, curLine);
                                System.out.println("Incorrect symbols in the following input path:\n" + curLine);
                            }
                        }
                    } catch (IOException e) {
                        System.out.println("Read error in file " + args[0]);
                    }
                } catch (FileNotFoundException e) {
                    System.out.println("Output file doesn't exist");
                }
            } catch (FileNotFoundException e) {
                System.out.println("Input file doesn't exist");
            } catch (UnsupportedEncodingException e) {
                System.out.println("UTF-8 encoding is unsupported");
            } catch (IOException e) {
                System.out.println("Close error");
            }
        }
    }
}
