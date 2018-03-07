package ru.ifmo.rain.vaksman.walk;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class FileVisitor extends SimpleFileVisitor<Path> {
    private Writer writer;
    private byte[] buf;

    FileVisitor(Writer writer, byte[] buf) {
        this.writer = writer;
        this.buf = buf;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        int hashValue = 0x811c9dc5;
        try (InputStream in = Files.newInputStream(file)) {
            int t;
            while ((t = in.read(buf)) >= 0) {
                hashValue = hash(hashValue, buf, t);
            }
        } catch(IOException e) {
            hashValue = 0;
            System.out.println("Error occurred during reading the following file: " + file + "\nReason: " + e.getMessage());
        } finally {
            writeData(hashValue, file.toString());
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
        writeData(0, file.toString());
        System.out.println("Unable to visit the following file: " + file + "\nReason: " + exc.getMessage());
        return FileVisitResult.CONTINUE;
    }

    private int hash(int h, final byte[] buf, int t) {
        for (int i = 0; i < t; ++i) {
            h = (h * 0x01000193) ^ (buf[i] & 0xff);
        }
        return h;
    }

    void writeData(int hash, String file) {
        try {
            writer.write(String.format("%08x", hash) + " " + file + System.lineSeparator());
        } catch (IOException e) {
            System.out.println("Unable to write in the output file:\n" + e.getMessage());
        }
    }
}
