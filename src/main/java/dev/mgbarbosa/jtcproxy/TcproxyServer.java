package dev.mgbarbosa.jtcproxy;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.rmi.AccessException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TcproxyServer {

    private static final Integer DEFAULT_BUFFER = (1024 * 1024) * 16; // 16MB

    private ServerSocketChannel server = null;
    private final int port;
    private final int bufferSize;
    private final InetAddress address;
    private final ExecutorService executorService;
    private final Logger logger = LoggerFactory.getLogger(TcproxyServer.class);

    public TcproxyServer(final InetAddress ipAddress, final int port) {
        this(ipAddress, port, DEFAULT_BUFFER);
    }

    public TcproxyServer(final InetAddress ipAddress, final int port, final int bufferSize) {
        this.port = port;
        this.address = ipAddress;
        this.bufferSize = bufferSize;
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
    }

    public void startServer() throws IOException {
        this.server = SelectorProvider.provider().openServerSocketChannel();
        this.server.configureBlocking(false);
        this.server.register(Selector.open(), SelectionKey.OP_ACCEPT);
        this.server.socket().bind(new InetSocketAddress(this.address, this.port));

        logger.info("Server running at {}", this.server.socket().getLocalSocketAddress());
    }

    public void stopServer() throws InterruptedException {
        this.executorService.close();
        final var result = this.executorService.awaitTermination(10000, TimeUnit.MILLISECONDS);
        if (!result) {
            throw new InterruptedException();
        }
    }

    public void startAccepting(final CancellationToken cancellationToken) throws IOException {
        this.executorService.submit(() -> {
            try {
                while (!cancellationToken.isCancellationRequested()) {
                    handleSocket(acceptSocket(cancellationToken), cancellationToken);
                }
            } catch (Exception ex) {
                throw new RuntimeException("failed to handle socket", ex);
            }

            logger.info("received cancellation request");
        });
    }

    private SocketChannel acceptSocket(final CancellationToken cancellationToken) throws Exception {
        SocketChannel acceptedSocket = null;
        while ((acceptedSocket = this.server.accept()) == null) {
            Thread.yield();
            cancellationToken.throwIfCancellationRequested();
        }

        return acceptedSocket;
    }

    private void handleSocket(final SocketChannel socket, final CancellationToken cancellationToken) {
        this.executorService.submit(() -> {
            try (final var client = new ClientSocketStream(socket, this.bufferSize)) {
                socket.configureBlocking(false);
                client.start(cancellationToken);
            } catch (IOException ex) {
                logger.error("failed when trying to handle client", ex);
            } catch (InterruptedException | ExecutionException e) {
                logger.error("received cancellation request.");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public int getListeningPort() {
        return this.server.socket().getLocalPort();
    }
}
