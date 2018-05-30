package ru.ifmo.rain.vaksman.udp;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.*;

public class HelloUDPServer implements HelloServer {
    static final int MAX_SIZE = 1000000;
    private DatagramSocket socket;
    private ExecutorService workers;
    private ExecutorService listener;
    private int receiveBufferSize;
    private int sendBufferSize;

    @Override
    public void start(int port, int threads) {
        if (threads <= 0 || port < 0) {
            throw new IllegalArgumentException("Amount of threads or port's number is incorrect");
        }
        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            System.err.println("Couldn't open socket with the specified port: " + e.getMessage());
            return;
        }
        try {
            receiveBufferSize = socket.getReceiveBufferSize();
            sendBufferSize = socket.getSendBufferSize();
        } catch (SocketException e) {
            System.err.println("UDP error occurred: " + e.getMessage());
            return;
        }
        workers = new ThreadPoolExecutor(
                threads,
                threads,
                0, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(MAX_SIZE),
                new ThreadPoolExecutor.DiscardPolicy());
        listener = Executors.newSingleThreadExecutor();
        listener.submit(this::listen);
    }

    @Override
    public void close() {
        listener.shutdownNow();
        workers.shutdown();
        try {
            if (!workers.awaitTermination(5, TimeUnit.SECONDS)) {
                System.err.println("Server has not terminated during 5 seconds - shutdown");
                workers.shutdownNow();
            }
        } catch (InterruptedException e) {
            System.err.println("Interrupted exception occurred during waiting for termination - shutdown: " + e.getMessage());
            workers.shutdownNow();
        }
        socket.close();
    }

    private void listen() {
        while (!Thread.currentThread().isInterrupted()) {
            DatagramPacket packet = new DatagramPacket(new byte[receiveBufferSize], receiveBufferSize);
            try {
                socket.receive(packet);
                workers.submit(new Worker(packet));
            } catch (IOException e) {
                System.err.println("Couldn't get a query: " + e.getMessage());
            }
        }
    }

    private class Worker implements Runnable {
        private DatagramPacket packet;

        private Worker(DatagramPacket packet) {
            this.packet = packet;
        }

        @Override
        public void run() {
            SocketAddress address = packet.getSocketAddress();
            String request = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
            String replyMessage = "Hello, " + request;
            try {
                sendReply(address, replyMessage);
            } catch (IOException e) {
                System.err.println("Couldn't send the reply: " + e.getMessage());
            }
        }

        private void sendReply(SocketAddress address, String replyMessage) throws IOException {
            byte[] bytes = replyMessage.getBytes(StandardCharsets.UTF_8);
            if (bytes.length > sendBufferSize) {
                throw new IOException("The message is too large");
            }
            DatagramPacket reply = new DatagramPacket(bytes, bytes.length, address);
            socket.send(reply);
        }
    }

    public static void main(String[] args) {
        if (args == null || args.length != 2 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Incorrect input format.\nUsage: HelloUDPServer <port number> <threads number>");
        }
        int port, threads;
        try {
            port = Integer.parseInt(args[0]);
            threads = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            IllegalArgumentException exception  = new IllegalArgumentException("Couldn't parse given numbers: " + e.getMessage());
            exception.addSuppressed(e);
            throw exception;
        }
        new HelloUDPServer().start(port, threads);
    }
}