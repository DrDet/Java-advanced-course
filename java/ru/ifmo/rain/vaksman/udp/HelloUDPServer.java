package ru.ifmo.rain.vaksman.udp;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HelloUDPServer implements HelloServer {
    private DatagramSocket socket;
    private ExecutorService receivers;
    private int receiveBufferSize;
    private int sendBufferSize;

    @Override
    public void start(int port, int threads) {
        if (threads <= 0 || port < 0 || port > 65535) {
            throw new IllegalArgumentException("Amount of threads or port's number is incorrect");
        }
        threads = Integer.min(threads, Runtime.getRuntime().availableProcessors());
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
        receivers = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            receivers.submit(new Receiver());
        }
    }

    @Override
    public void close() {
        receivers.shutdownNow();
        socket.close();
    }

    private class Receiver implements Runnable {
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    DatagramPacket query = getQuery();
                    String replyMessage = "Hello, " + (new String(query.getData(), 0, query.getLength()));
                    try {
                        sendReply(new InetSocketAddress(query.getAddress(), query.getPort()), replyMessage);
                    } catch (IOException e) {
                        System.err.println("Couldn't send the reply: " + e.getMessage());
                    }
                } catch (IOException e) {
                    System.err.println("Couldn't get a query: " + e.getMessage());
                }
            }
        }

        private DatagramPacket getQuery() throws IOException {
            DatagramPacket query = new DatagramPacket(new byte[receiveBufferSize], receiveBufferSize);
            socket.receive(query);
            return query;
        }

        private void sendReply(InetSocketAddress address, String replyMessage) throws IOException {
            byte[] bytes = replyMessage.getBytes();
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
