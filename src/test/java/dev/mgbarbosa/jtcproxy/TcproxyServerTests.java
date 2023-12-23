package dev.mgbarbosa.jtcproxy;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.Test;

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
    public void shouldAnswerPing() throws IOException, InterruptedException {
        // Arrange
        final var cancellationTokenSource = new CancellationTokenSource();
        final var server = setupServer(cancellationTokenSource.getToken());

        // Act
        final var socket = new Socket("127.0.0.1", server.getListeningPort());
        final var outputStream = socket.getOutputStream();
        final var inputStream = socket.getInputStream();

        final var message = "TCPROXY/1.0\r\nPING\r\n".getBytes(StandardCharsets.UTF_8);

        outputStream.write(message);
        outputStream.flush();

        final var buffer = new byte[1024];
        final var readBytes = inputStream.read(buffer);
        final var byteBuffer = Arrays.copyOfRange(buffer, 0, readBytes);

        final var expectedAnswer = "TCPROXY/1.0\r\nPONG\r\n".getBytes(StandardCharsets.UTF_8);

        // Assert
        assertTrue(readBytes > 0);
        assertTrue(Arrays.equals(expectedAnswer, byteBuffer));

        // Cleanup
        cancellationTokenSource.cancel();
        socket.close();
        server.stopServer();
    }

    private TcproxyServer setupServer(final CancellationToken cancellationToken) throws IOException {
        final var serverAddress = Inet4Address.getByName("127.0.0.1");
        final var server = new TcproxyServer(serverAddress, 0);
        server.startServer();
        server.startAccepting(cancellationToken);


        return server;
    }
}
