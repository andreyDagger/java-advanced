package info.kgeorgiy.ja.matveev.hello;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;

import info.kgeorgiy.ja.matveev.hello.Utils.*;

public class HelloUDPNonblockingServer extends AbstractHelloUDPServer {
    private static final int BUFFER_SIZE = 1024;
    private Selector selector;
    private final ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

    /**
     * This method just does: {@code try (HelloUDPNonblockingServer server = new HelloUDPNonblockingServer()) {
     * server.mainImpl(args);
     * }}.
     * See {@link AbstractHelloUDPServer#mainImpl(String[])} documentation for more information.
     *
     * @param args Command line arguments
     */
    public static void main(final String[] args) {
        try (final HelloUDPNonblockingServer server = new HelloUDPNonblockingServer()) {
            server.mainImpl(args);
        }
    }

    @Override
    public void start(final int threads, final Map<Integer, String> ports) {
        if (threads <= 0) {
            throw new RuntimeException("threads must be greater than 0");
        }
        receiversPool = Executors.newFixedThreadPool(1);
        workersPool = Executors.newFixedThreadPool(threads);
        super.start(threads, ports);
    }

    protected void startImpl(final Map<Integer, String> ports) throws IOException {
        selector = Selector.open();

        for (final var entry : ports.entrySet()) {
            final int port = entry.getKey();
            final String answerFormat = entry.getValue();

            final DatagramChannel channel = selector.provider().openDatagramChannel();
            channel.configureBlocking(false);
            channel.bind(new InetSocketAddress(port));
            channel.register(selector, SelectionKey.OP_READ, new Attachment(answerFormat));
            resources.add(channel);
        }

        receiversPool.submit(() -> {
            while (!Thread.interrupted()) {
                try {
                    selector.select(this::handler, SOCKET_TIMEOUT_MS); // :NOTE: to constant - fixed
                } catch (final ClosedSelectorException e) {
                    break; // close called
                } catch (final RuntimeException | IOException e) {
                    System.out.println("ERROR: " + e.getMessage());
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void handler(final SelectionKey key) {
        try {
            final Attachment attachment = (Attachment) key.attachment();
            final DatagramChannel channel = (DatagramChannel) key.channel();
            if (key.isReadable()) {
                buffer.clear();
                final SocketAddress address = channel.receive(buffer);
                buffer.flip();
                final String receivedMessage = StandardCharsets.UTF_8.decode(buffer).toString();
                workersPool.submit(() -> {
                    final String sendMessage = attachment.answerFormat.replace("$", receivedMessage);
                    attachment.add(new SendInfo(ByteBuffer.wrap(sendMessage.getBytes(StandardCharsets.UTF_8)), address));
                    key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                });
            } else if (key.isWritable()) {
                final var info = attachment.poll();
                channel.send(Objects.requireNonNull(info).buffer, info.address);
                if (attachment.isEmpty()) {
                    key.interestOps(SelectionKey.OP_READ);
                }
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // :NOTE: Объединить - fixed
    @Override
    public void close() {
        try {
            selector.close();
        } catch (final IOException e) {
            System.err.println("Couldn't close selector: " + e.getMessage());
        }
        super.close();
    }

    private static class Attachment {
        private final ConcurrentLinkedQueue<SendInfo> queue = new ConcurrentLinkedQueue<>();
        private final String answerFormat;

        private Attachment(final String answerFormat) {
            this.answerFormat = answerFormat;
        }

        private void add(final SendInfo info) {
            queue.add(info);
        }

        private SendInfo poll() {
            return queue.poll();
        }

        private boolean isEmpty() {
            return queue.isEmpty();
        }
    }
}
