package info.kgeorgiy.ja.matveev.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static info.kgeorgiy.ja.matveev.hello.Utils.closeExecutor;
import static info.kgeorgiy.ja.matveev.hello.Utils.correct;

/**
 * Basic implementation of client, that talks with udp echo server
 *
 * @author Andrey Matveev
 * @version 21
 * @see HelloClient
 * @since 21
 */
public class HelloUDPClient extends AbstractHelloUDPClient {
    private static final long ONE_WORK_TIME = 3; // in seconds

    /**
     * This method just does: {@code new HelloUDPClient().mainImpl(args);}.
     * See {@link AbstractHelloUDPClient#mainImpl(String[])} documentation for more information.
     *
     * @param args Command line arguments
     */
    public static void main(final String[] args) {
        new HelloUDPClient().mainImpl(args);
    }

    @Override
    public void run(final String host, final int port, final String prefix, final int threads, final int requests) {
        final InetAddress address;
        try {
            address = InetAddress.getByName(host);
        } catch (final UnknownHostException e) {
            System.err.println("Couldn't get host address" + e.getMessage());
            return;
        }
        final ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (int i = 1; i <= threads; ++i) {
            final int finalI = i;
            final Runnable client = () -> {
                try (final DatagramSocket socket = new DatagramSocket()) {
                    socket.setSoTimeout(RECEIVE_TIMEOUT_MS);
                    final byte[] receiveData = new byte[socket.getReceiveBufferSize()];
                    final DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

                    for (int j = 1; j <= requests; ++j) {
                        while (true) {
                            final String sendMessage = prefix + finalI + "_" + j;
                            final DatagramPacket sendPacket = Utils.convertStringToDatagramPacket(sendMessage, address, port);
                            try {
                                socket.send(sendPacket);
                                socket.receive(receivePacket);
                                final String receivedMessage = Utils.convertDatagramPacketToString(receivePacket);
                                if (correct(finalI, j, receivedMessage)) {
                                    System.out.println("OK: " + "SEND: " + sendMessage + "; RECEIVED: " + receivedMessage);
                                    break;
                                } else {
                                    System.out.println("BAD: " + "SEND: " + sendMessage + "; RECEIVED: " + receivedMessage);
                                }
                            } catch (final IOException e) {
                                System.err.println("ERROR: " + e.getMessage());
                            }
                        }
                    }
                } catch (final SocketException e) {
                    System.err.println("Couldn't create socket: " + e.getMessage());
                }
            };
            pool.submit(client);
        }
        closeExecutor(pool, threads * requests * ONE_WORK_TIME, TimeUnit.SECONDS);
    }
}
