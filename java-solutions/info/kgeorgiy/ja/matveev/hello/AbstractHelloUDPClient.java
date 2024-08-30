package info.kgeorgiy.ja.matveev.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.util.Arrays;
import java.util.Objects;

/**
 * Abstract implementation of {@link HelloClient}. Have only realisation of {@code main} function
 *
 * @author Andrey Matveev
 * @since 21
 */
abstract public class AbstractHelloUDPClient implements HelloClient {
    protected static int RECEIVE_TIMEOUT_MS = 100; // In milliseconds

    /**
     * Runs client.
     * Usage: args = {ip-address-of-server, port, prefix, threads, requests}
     * ip-address-of-server - is Ip address of server to which we send messages
     * port - port on which server is located
     * prefix - prefix that will be added to received mesdage
     * threads - how much threads wiil be used in client
     * requests - number of requests that will be made by one thread
     *
     * @param args String array that has format, described upper
     */
    protected void mainImpl(final String[] args) {
        if (args == null || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("args and his elements must be non-null");
            return;
        }
        if (args.length < 5) {
            printUsage();
            return;
        }
        try {
            final String host = args[0];
            final int port = Integer.parseInt(args[1]);
            final String prefix = args[2];
            final int threads = Integer.parseInt(args[3]);
            final int requests = Integer.parseInt(args[4]);
            if (port < 0) {
                System.err.println("port must be non-negative integer");
                return;
            }
            if (threads <= 0) {
                System.err.println("threads must be positive integer");
                return;
            }
            if (requests < 0) {
                System.err.println("requests must be non-negative integer");
                return;
            }
            run(host, port, prefix, threads, requests);
        } catch (final NumberFormatException e) {
            printUsage();
        }
    }

    private static void printUsage() {
        System.err.println("Usage: {ip-address-of-server} {port} {prefix} {threads} {requests}");
    }
}
