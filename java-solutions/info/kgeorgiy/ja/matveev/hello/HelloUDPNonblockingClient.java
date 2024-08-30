package info.kgeorgiy.ja.matveev.hello;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static info.kgeorgiy.ja.matveev.hello.Utils.closeResources;
import static info.kgeorgiy.ja.matveev.hello.Utils.correct;

public class HelloUDPNonblockingClient extends AbstractHelloUDPClient {
    private final int BUFFER_SIZE = 1024;
    private final ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

    private record Attachment(String prefix, int threadNum, int requestNum, SocketAddress address, int maxRequests) {
    }

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
        if (threads <= 0) {
            throw new RuntimeException("threads must be greater than 0");
        }

        final InetSocketAddress address;
        try {
            address = new InetSocketAddress(InetAddress.getByName(host), port);
        } catch (final IllegalArgumentException | UnknownHostException e) {
            System.err.println("Couldn't reach host: " + e.getMessage());
            return;
        }

        final Selector selector;
        try {
            selector = Selector.open();
        } catch (final IOException e) {
            System.err.println("Can't open selector: " + e.getMessage());
            return;
        }

        List<DatagramChannel> channels = new ArrayList<>();
        for (int i = 1; i <= threads; ++i) {
            try {
                // :NOTE: who closes? - fixed
                final DatagramChannel channel = selector.provider().openDatagramChannel();
                channel.configureBlocking(false);
                channel.connect(address);
                channel.register(selector, SelectionKey.OP_WRITE, new Attachment(prefix, i, 1, address, requests));
                channels.add(channel);
            } catch (final IOException e) {
                try {
                    closeResources(channels);
                } catch (final IOException e1) {
                    System.err.println("ERROR: " + e1.getMessage());
                }
                System.err.println("ERROR: " + e.getMessage());
                return;
            }
        }

        try {
            while (!selector.keys().isEmpty()) {
                if (selector.select(this::handle, RECEIVE_TIMEOUT_MS) == 0) {
                    // Receive timeouted, so sending again
                    selector.keys().forEach(k -> k.interestOps(SelectionKey.OP_WRITE));
                }
            }
        } catch (final IOException | UncheckedIOException e) {
            System.err.println("ERROR: " + e.getMessage());
        } finally {
            try {
                closeResources(channels);
            } catch (final IOException e) {
                System.err.println("ERROR: " + e.getMessage());
            }
        }
    }

    private void handle(final SelectionKey key) {
        if (!key.isValid()) {
            return;
        }

        try {
            final var channel = (DatagramChannel) key.channel();
            final Attachment info = (Attachment) key.attachment();
            if (key.isWritable()) {
                final String sendMessage = info.prefix + info.threadNum + "_" + info.requestNum;
                final ByteBuffer buffer = ByteBuffer.wrap(sendMessage.getBytes(StandardCharsets.UTF_8));

                try {
                    channel.send(buffer, info.address);
                    System.out.println("SEND: " + sendMessage);
                } catch (final IOException e) {
                    System.err.println("Couldn't send message: " + e.getMessage());
                    return;
                }
                key.interestOps(SelectionKey.OP_READ);
            } else if (key.isReadable()) {
                buffer.clear();
                try {
                    channel.receive(buffer);
                    buffer.flip();
                    final String receivedMessage = StandardCharsets.UTF_8.decode(buffer).toString();
                    if (correct(info.threadNum, info.requestNum, receivedMessage)) {
                        System.out.println("OK: " + receivedMessage);
                    } else {
                        System.out.println("BAD: " + receivedMessage);
                        key.interestOps(SelectionKey.OP_WRITE);
                        return;
                    }
                } catch (final IOException e) {
                    System.err.println("ERROR: " + e);
                    key.interestOps(SelectionKey.OP_WRITE);
                    return;
                }
                if (info.requestNum == info.maxRequests) {
                    channel.close();
                    key.cancel();
                } else {
                    key.attach(new Attachment(info.prefix, info.threadNum, info.requestNum + 1, info.address, info.maxRequests));
                    key.interestOps(SelectionKey.OP_WRITE);
                }
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
