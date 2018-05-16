package ru.ifmo.rain.vaksman.udp;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloUDPServer implements HelloServer {
    private DatagramSocket socket;
    private ExecutorService workers;
    private ExecutorService listener;
    private int receiveBufferSize;
    private int sendBufferSize;

    @Override
    public void start(int port, int threads) {
        if (threads <= 0 || port < 0 || port > 65535) {
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
        listener = Executors.newSingleThreadExecutor();
        workers = Executors.newFixedThreadPool(threads);
        listener.submit(this::listen);
    }

    @Override
    public void close() {
        workers.shutdownNow();
        listener.shutdownNow();
        socket.close();
    }

    private void listen()  {
        DatagramPacket packet = new DatagramPacket(new byte[receiveBufferSize], receiveBufferSize);
        while (!Thread.currentThread().isInterrupted()) {
            try {
                socket.receive(packet);
                workers.submit(new Worker(new InetSocketAddress(packet.getAddress(), packet.getPort()), new String(packet.getData(), 0, packet.getLength())));
            } catch (IOException e) {
                System.err.println("Couldn't get a query: " + e.getMessage());
            }
        }
    }

    private class Worker implements Runnable {
        private final InetSocketAddress address;
        private final String request;

        private Worker(InetSocketAddress address, String request) {
            this.address = address;
            this.request = request;
        }

        @Override
        public void run() {
            String replyMessage = "Hello, " + request;
            try {
                sendReply(address, replyMessage);
            } catch (IOException e) {
                System.err.println("Couldn't send the reply: " + e.getMessage());
            }
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
