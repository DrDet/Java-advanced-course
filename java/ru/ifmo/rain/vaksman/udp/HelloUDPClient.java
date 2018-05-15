package ru.ifmo.rain.vaksman.udp;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

public class HelloUDPClient implements HelloClient {
    private ExecutorService senders;
    private InetSocketAddress serverAddress;

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        if (threads <= 0 || port < 0 || port > 65535) {
            throw new IllegalArgumentException("Amount of threads or port's number is incorrect");
        }
        try {
            serverAddress = new InetSocketAddress(InetAddress.getByName(host), port);
        } catch (UnknownHostException e) {
            System.err.println("Couldn't find IP address for the specified host: " + e.getMessage());
            return;
        }
        senders = Executors.newFixedThreadPool(threads);
        List<Future> res = new ArrayList<>();
        for (int i = 0; i < threads; ++i) {
            res.add(senders.submit(new Sender(i, requests, prefix)));
        }
        res.forEach(x -> {
            try {
                x.get();
            } catch (InterruptedException e) {
                System.err.println("The task interrupted: " + e.getMessage());
            } catch (ExecutionException e) {
                System.err.println("Exception during computation occurred: " + e.getMessage());
            }
        });
        senders.shutdownNow();
    }

    private class Sender implements Runnable {
        private final int number;
        private final int requests;
        private final String queryPref;
        private int receiveBufferSize;
        private int sendBufferSize;
        private DatagramSocket socket;

        private Sender(int number, int requests, String queryPref) {
            this.number = number;
            this.requests = requests;
            this.queryPref = queryPref;
        }

        @Override
        public void run() {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setSoTimeout(5000);
                receiveBufferSize = socket.getReceiveBufferSize();
                sendBufferSize = socket.getSendBufferSize();
                this.socket = socket;
                for (int i = 0; i < requests; i++) {
                    String query = queryPref + number + "_" + i, reply = "";
                    do {
                        try {
                            sendQuery(serverAddress, query);
                        System.err.println(Thread.currentThread().getName() + " ??? " + query);
                            reply = receiveReply();
                        System.err.println(Thread.currentThread().getName() + " !!! " + reply);
                        } catch (IOException e) {
                            System.err.println("Couldn't process the query: " + e.getMessage() + "\nTrying again...");
                        }
                    } while (reply.isEmpty() || !isProcessed(query, reply));
                    System.err.println(reply);
                }
            } catch (SocketException e) {
                System.err.println("Socket error occurred: " + e.getMessage());
            }
        }

        private void sendQuery(InetSocketAddress address, String message) throws IOException {
            if (message.getBytes().length > sendBufferSize) {
                throw new IOException("The message is too large");
            }
            DatagramPacket query = new DatagramPacket(message.getBytes(), message.getBytes().length, address);
            socket.send(query);
        }

        private String receiveReply() throws IOException {
            DatagramPacket reply = new DatagramPacket(new byte[receiveBufferSize], receiveBufferSize);
            socket.receive(reply);
            return new String(reply.getData(), 0, reply.getLength());
        }

        private boolean isProcessed(String query, String reply) {
            return !reply.equals(query) && reply.contains(query);
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
