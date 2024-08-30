package info.kgeorgiy.ja.matveev.hello;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * Utilities for {@link HelloUDPServer} and {@link HelloUDPClient}
 *
 * @author Andrey Matveev
 * @since 21
 */
public class Utils {
    private Utils() {}

    /**
     * Decodes {@code packet} to {@link DatagramPacket} in UTF-8
     *
     * @param packet Packet to decode
     * @return Decoded packet
     */
    static String convertDatagramPacketToString(DatagramPacket packet) {
        return new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
    }

    /**
     * Encodes {@code message} to {@link DatagramPacket}
     *
     * @param message Message to encode
     * @return Encoded message
     */
    static DatagramPacket convertStringToDatagramPacket(String message) {
        byte[] sendData = message.getBytes(StandardCharsets.UTF_8);
        return new DatagramPacket(sendData, sendData.length);
    }

    /**
     * Encodes {@code message} to {@link DatagramPacket}.
     * Same as {@link Utils#convertStringToDatagramPacket(String)} but also specifies on which {@code address} and {@code port} message will be sent.
     *
     * @param message Message to encode
     * @param address Address on which message will be sent
     * @param port Port on which message will be sent
     * @return Encoded message
     */
    static DatagramPacket convertStringToDatagramPacket(String message, InetAddress address, int port) {
        byte[] sendData = message.getBytes(StandardCharsets.UTF_8);
        return new DatagramPacket(sendData, sendData.length, address, port);
    }

    static class SendInfo {
        final ByteBuffer buffer;
        final SocketAddress address;

        public SendInfo(ByteBuffer buffer, SocketAddress address) {
            this.buffer = buffer;
            this.address = address;
        }
    }

    private static int lastIndexIf(String s, Predicate<Character> p) {
        for (int i = s.length() - 1; i >= 0; --i) {
            if (p.test(s.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    static boolean correct(int ei, int ej, String received) {
        if (received.isEmpty() || Character.isDigit(received.charAt(0))) {
            return false;
        }
        List<Character> blanks = List.of('_', '-');
        int r = lastIndexIf(received, blanks::contains) + 1;
        int l = lastIndexIf(received.substring(0, r), c -> !blanks.contains(c)) + 1;
        int i = lastIndexIf(received.substring(0, l), c -> !Character.isDigit(c)) + 1;

        // _ _ _ _ _ 2 3 3 _ _ _ _ _ 4 5 6
        //           i     l         r

        try {
            int ai = Integer.parseInt(received.substring(i, l));
            int aj = Integer.parseInt(received.substring(r));
            return ai == ei && aj == ej;
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            return false;
        }
    }

    static void closeExecutor(ExecutorService pool, long timeout, TimeUnit timeUnit) {
        pool.shutdown();
        boolean done = true;
        try {
            if (!pool.awaitTermination(timeout, timeUnit)) {
                done = false;
            }
        } catch (InterruptedException e) {
            done = false;
        }
        if (!done) {
            System.err.println("WARNING: Non-graceful shutdown for " + pool);
            pool.shutdownNow();
        }
    }

    static void closeResources(List<? extends Closeable> resources) throws IOException {
        try {
            resources.forEach(c -> {
                try {
                    c.close();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }
}
