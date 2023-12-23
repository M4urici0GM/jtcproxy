package dev.mgbarbosa.jtcproxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.concurrent.Executors;


import org.junit.Test;

import dev.mgbarbosa.jtcproxy.cancellation.CancellationToken;
import dev.mgbarbosa.jtcproxy.cancellation.CancellationTokenSource;
import dev.mgbarbosa.jtcproxy.server.PortProvider;
import dev.mgbarbosa.jtcproxy.server.ServerPort;

public class TcproxyServerTests {

    @Test
    public void serverShouldBeListening() throws Exception {
        // Arrange
        final var cancellationTokenSource = new CancellationTokenSource();
        final var server = setupServer(cancellationTokenSource.getToken());

        // Act
        final var socket = new Socket("127.0.0.1", server.getListeningPort());

        // Assert
        assertTrue(socket.isConnected());

        // Cleanup
        cancellationTokenSource.cancel();
        server.stopServer();
        socket.close();
    }

    @Test
    public void shouldBeAbleToDoHandshake() throws IOException, InterruptedException {
        // Arrange
        final var cancellationTokenSource = new CancellationTokenSource();
        final var server = setupServer(cancellationTokenSource.getToken());

        // Act
        final var selector = Selector.open();
        final var serverAddress = Inet4Address.getByName("127.0.0.1");
        final var socket = SocketChannel.open(new InetSocketAddress(serverAddress, server.getListeningPort()));
        socket.configureBlocking(false);
        socket.register(selector, SelectionKey.OP_READ);
        socket.register(selector, SelectionKey.OP_WRITE);


        final var inputMessage = ByteBuffer.wrap(new byte[] {
                0x01, // Version
                0x01, // ClientHello
                0x00, 0x00, // PayloadSize (16 Bit Unsigned int)
        });
        while ((socket.write(inputMessage)) != 0) {}

        final var buffer = ByteBuffer.allocate(1024);
        var readBytes = 0;
        while ((readBytes = socket.read(buffer)) == 0) {
            assertTrue(readBytes >= 0);
        }

        assertTrue(readBytes > 0);

        final var byteBuffer = Arrays.copyOfRange(buffer.array(), 0, readBytes);
        final var expectedAnswer = new byte[] {
                0x01, // Version
                0x02, // ServerHello
                0x00, 0x02, // PayloadSize (16 Bit Unsigned int)
                0x10, 0x00
        };

        // Assert
        assertTrue(readBytes > 0);
        assertTrue(Arrays.equals(expectedAnswer, byteBuffer));

        // Cleanup
        cancellationTokenSource.cancel();
        socket.close();
        server.stopServer();
    }

    public void shouldSpawnRemoteConnection() throws Exception {
        // Arrange
        final var cancellationTokenSource = new CancellationTokenSource();
        final var portManager = mock(PortProvider.class);
        final var port = mock(ServerPort.class);
        final var server = setupServer(portManager, cancellationTokenSource.getToken());

        when(portManager.get()).thenReturn(port);
        when(port.getPort()).thenReturn(3337);

        // Act
        final var socket = new Socket("127.0.0.1", server.getListeningPort());
        doHandshake(socket);

        // Cleanup
        cancellationTokenSource.cancel();
        socket.close();
        server.stopServer();
    }

    private void doHandshake(final Socket socket) throws IOException {
        final var outputStream = socket.getOutputStream();
        final var inputStream = socket.getInputStream();
        final var inputMessage = new byte[] {
                0x01, // Version
                0x03, // ClientHello
                0x00, 0x00, // Payload size
        };

        outputStream.write(inputMessage);
        outputStream.flush();

        final var byteBuffer = new byte[256];
        final var bytesRead = inputStream.read(byteBuffer, 0, byteBuffer.length);
        assertTrue(bytesRead > 0);

        final var readBuffer = Arrays.copyOfRange(byteBuffer, 0, bytesRead);
        final var expectedBuffer = new byte[] {
                0x01, // Version
                0x02, // ServerHello
                0x00, 0x02, // Payload Size,
                0x10, 0x00 // 4096 (16 bit unsigned int)
        };

        assertEquals(expectedBuffer, readBuffer);
    }

    private TcproxyServer setupServer(final CancellationToken cancellationToken) throws IOException {
        return setupServer(mock(PortProvider.class), cancellationToken);
    }

    private TcproxyServer setupServer(final PortProvider portProvider, final CancellationToken cancellationToken)
            throws IOException {
        final var serverAddress = Inet4Address.getByName("127.0.0.1");
        final var server = TcproxyServer.builder()
                .portProvider(portProvider)
                .bufferSize(1024 * 1024)
                .executorService(Executors.newVirtualThreadPerTaskExecutor())
                .port(0)
                .address(serverAddress)
                .build();

        server.startServer();
        server.startAccepting(cancellationToken);

        return server;
    }
}
