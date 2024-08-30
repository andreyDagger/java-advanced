package info.kgeorgiy.ja.matveev.hello;

import info.kgeorgiy.java.advanced.hello.NewHelloServer;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static info.kgeorgiy.ja.matveev.hello.Utils.closeExecutor;

abstract public class AbstractHelloUDPServer implements NewHelloServer {
    protected ExecutorService receiversPool;
    protected ExecutorService workersPool;
    protected final List<Closeable> resources = new ArrayList<>();
    protected static final int SOCKET_TIMEOUT_MS = 40;
    protected static final int CLOSE_TIMEOUT_SECONDS = 10;

    /**
     * Runs server.
     * Usage: args = {port, threads}
     * port - port on which server is located
     * threads - how much threads wiil be used in client
     *
     * @param args String array that has format, described upper
     */
    protected void mainImpl(final String[] args) {
        if (args == null || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("args and his elements must be non-null");
            return;
        }
        if (args.length < 2) {
            printUsage();
            return;
        }
        try {
            final int port = Integer.parseInt(args[0]);
            final int threads = Integer.parseInt(args[1]);
            if (port < 0) {
                System.err.println("port must be non-negative integer");
                return;
            }
            if (threads <= 0) {
                System.err.println("threads must be positive integer");
                return;
            }
            start(port, threads);
            final Scanner scanner = new Scanner(System.in);
            while (scanner.hasNext()) {
                scanner.next();
            }
            scanner.close();
        } catch (final NumberFormatException e) {
            printUsage();
        }
    }

    private static void printUsage() {
        System.err.println("Usage: {port} {threads}");
    }

    @Override
    public void start(final int threads, final Map<Integer, String> ports) {
        try {
            startImpl(ports);
        } catch (final IOException e) {
            System.err.println("ERROR: " + e.getMessage());
        }
    }

    abstract protected void startImpl(Map<Integer, String> ports) throws IOException;

    @Override
    public void close() {
        resources.forEach(c -> {
            try {
                c.close();
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        closeExecutor(receiversPool, CLOSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        closeExecutor(workersPool, CLOSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }
}
