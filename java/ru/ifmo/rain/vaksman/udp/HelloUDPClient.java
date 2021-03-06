package ru.ifmo.rain.vaksman.udp;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class HelloUDPClient implements HelloClient {
    private ExecutorService senders;
    private InetSocketAddress serverAddress;

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        if (threads <= 0 || port < 0) {
            throw new IllegalArgumentException("Amount of threads or port's number is incorrect");
        }
        serverAddress = new InetSocketAddress(host, port);
        senders = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            senders.submit(new Sender(i, requests, prefix));
        }
        senders.shutdown();
        try {
            if (!senders.awaitTermination(5, TimeUnit.MINUTES)) {
                System.err.println("It's not terminated during 5 minutes - shutdown");
                senders.shutdownNow();
            }
        } catch (InterruptedException e) {
            System.err.println("Interrupted during waiting of termination - shutdown: " + e.getMessage());
            senders.shutdownNow();
        }
    }

    private class Sender implements Runnable {
        private final int number;
        private final int requests;
        private final String queryPref;
        private int receiveBufferSize;
        private int sendBufferSize;
        private DatagramSocket socket;
        private DatagramPacket packet;

        private Sender(int number, int requests, String queryPref) {
            this.number = number;
            this.requests = requests;
            this.queryPref = queryPref;
        }

        @Override
        public void run() {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setSoTimeout(1000);
                receiveBufferSize = socket.getReceiveBufferSize();
                sendBufferSize = socket.getSendBufferSize();
                this.socket = socket;
                this.packet = new DatagramPacket(new byte[receiveBufferSize], receiveBufferSize);
                for (int i = 0; i < requests; i++) {
                    String query = queryPref + number + "_" + i;
                    String reply = "";
                    do {
                        try {
                            sendQuery(serverAddress, query);
                            reply = receiveReply();
                        } catch (IOException e) {
                            System.err.println("Couldn't process the query: " + e.getMessage() + "\nTrying again...");
                        }
                    } while (reply.isEmpty() || !isProcessed(query, reply));
                    System.out.println(reply);
                }
            } catch (SocketException e) {
                System.err.println("Socket error occurred: " + e.getMessage());
            }
        }

        private void sendQuery(InetSocketAddress address, String message) throws IOException {
            if (message.getBytes(StandardCharsets.UTF_8).length > sendBufferSize) {
                throw new IOException("The message is too large");
            }
            DatagramPacket query = new DatagramPacket(message.getBytes(StandardCharsets.UTF_8), message.getBytes(StandardCharsets.UTF_8).length, address);
            socket.send(query);
        }

        private String receiveReply() throws IOException {
            socket.receive(packet);
            return new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
        }

        private boolean isProcessed(String query, String reply) {
            return reply.matches(".*" + Pattern.quote(query) + "(|\\p{Space}.*)");
        }
    }

    public static void main(String[] args) {
        if (args == null || args.length != 5 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Incorrect input format.\nUsage: HelloUDPClient <host name> <port number> <query> <threads number> <requests per thread number>");
        }
        int port, threads, requests;
        try {
            port = Integer.parseInt(args[1]);
            threads = Integer.parseInt(args[3]);
            requests = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            IllegalArgumentException exception = new IllegalArgumentException("Couldn't parse given numbers: " + e.getMessage());
            exception.addSuppressed(e);
            throw exception;
        }
        new HelloUDPClient().run(args[0], port, args[2], threads, requests);
    }
}