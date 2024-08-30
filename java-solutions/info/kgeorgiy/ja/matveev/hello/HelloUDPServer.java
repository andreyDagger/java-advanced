package info.kgeorgiy.ja.matveev.hello;

import info.kgeorgiy.java.advanced.hello.NewHelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * Basic implementation of udp echo server.
 *
 * @author Andrey Matveev
 * @version 21
 * @see NewHelloServer
 * @since 21
 */
public class HelloUDPServer extends AbstractHelloUDPServer {

    /**
     * This method just does: {@code try (HelloUDPServer server = new HelloUDPServer()) {
     * server.mainImpl(args);
     * }}.
     * See {@link AbstractHelloUDPServer#mainImpl(String[])} documentation for more information.
     *
     * @param args Command line arguments
     */
    public static void main(final String[] args) {
        try (final HelloUDPServer server = new HelloUDPServer()) {
            server.mainImpl(args);
        }
    }

    @Override
    public void start(final int threads, final Map<Integer, String> ports) {
        if (threads <= 0) {
            throw new RuntimeException("threads must be greater than 0");
        }
        receiversPool = Executors.newFixedThreadPool(Math.max(1, ports.size()));
        workersPool = Executors.newFixedThreadPool(threads);
        super.start(threads, ports);
    }

    protected void startImpl(final Map<Integer, String> ports) throws IOException {
        for (final var entry : ports.entrySet()) {
            final DatagramSocket socket = new DatagramSocket(entry.getKey());
            resources.add(socket);
            final int bufferSize = socket.getReceiveBufferSize();
            final Runnable receiver = getReceiver(entry.getValue(), socket, bufferSize);
            receiversPool.submit(receiver);
        }
    }

    private Runnable getReceiver(final String answerFormat, final DatagramSocket socket, final int bufferSize) {
        final byte[] receiveData = new byte[bufferSize];

        return () -> {
            while (!Thread.interrupted() && !socket.isClosed()) {
                final DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                try {
                    socket.receive(receivePacket);
                } catch (final IOException e) {
                    if (!socket.isClosed()) {
                        System.err.println(e.getMessage());
                    }
                    return;
                }
                final String receivedMessage = Utils.convertDatagramPacketToString(receivePacket);
                final Runnable worker = () -> {
                    final String sendMessage = answerFormat.replace("$", receivedMessage);
                    final DatagramPacket sendPacket = Utils.convertStringToDatagramPacket(sendMessage);
                    sendPacket.setSocketAddress(receivePacket.getSocketAddress());

                    try {
                        socket.send(sendPacket);
                    } catch (final IOException e) {
                        if (!socket.isClosed()) {
                            System.err.println(e.getMessage());
                        }
                    }
                };
                workersPool.submit(worker);
            }
        };
    }
}
