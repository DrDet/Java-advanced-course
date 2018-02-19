package ru.ifmo.rain.vaksman.walk;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class FileVisitor extends SimpleFileVisitor<Path> {
    private Writer writer;
    private byte[] buf;

    FileVisitor(Writer writer, byte[] buf) {
        super();
        this.writer = writer;
        this.buf = buf;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
            throws IOException
    {
        int hashValue = 0x811c9dc5;
        try (InputStream in = new FileInputStream(file.toString())) {
            int t;
            while ((t = in.read(buf)) >= 0) {
                hashValue = hash(hashValue, buf, t);
            }
        } catch(IOException e) {
            hashValue = 0;
            System.out.println("Read error in file " + file);
        } finally {
            writeData(hashValue, file.toString());
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc)
            throws IOException
    {
        writeData(0, file.toString());
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
            System.out.println("Write error");
        }
    }
}