package dev.mgbarbosa.jtcproxy;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.mgbarbosa.jtcproxy.cancellation.CancellationToken;
import dev.mgbarbosa.jtcproxy.client.ClientSocketStream;
import dev.mgbarbosa.jtcproxy.server.DefaultPortProvider;
import dev.mgbarbosa.jtcproxy.server.PortProvider;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TcproxyServer {

    private static final Integer DEFAULT_BUFFER = (1024 * 1024) * 16; // 16MB

    private ServerSocketChannel server;
    private final int port;
    private final int bufferSize;
    private final InetAddress address;
    private final ExecutorService executorService;
    private final PortProvider portProvider;
    private final Logger logger = LoggerFactory.getLogger(TcproxyServer.class);

    public static TcproxyServer create(final InetAddress ipAddress, final int port) {
        return TcproxyServer.builder()
                .address(ipAddress)
                .port(port)
                .bufferSize(DEFAULT_BUFFER)
                .executorService(Executors.newVirtualThreadPerTaskExecutor())
                .portProvider(DefaultPortProvider.getDefault())
                .build();
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
        if (Objects.isNull(this.server)) {
            throw new IllegalStateException("Server is not initialized yet. call startServer() before.");
        }

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
            final var clientBuilder = ClientSocketStream.builder()
                    .client(socket)
                    .clientBuffer(ByteBuffer.allocateDirect(this.bufferSize))
                    .portProvider(portProvider);

            try (final var client = clientBuilder.build()) {
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
