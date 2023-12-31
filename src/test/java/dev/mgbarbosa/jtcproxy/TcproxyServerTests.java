package dev.mgbarbosa.jtcproxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;

import org.junit.Test;

import dev.mgbarbosa.jtcproxy.cancellation.CancellationToken;
import dev.mgbarbosa.jtcproxy.cancellation.CancellationTokenSource;
import dev.mgbarbosa.jtcproxy.exceptions.BufferIncompleteException;
import dev.mgbarbosa.jtcproxy.protocol.MessageType;
import dev.mgbarbosa.jtcproxy.protocol.ProtocolVersion;
import dev.mgbarbosa.jtcproxy.server.PortProvider;
import dev.mgbarbosa.jtcproxy.server.ServerPort;
import dev.mgbarbosa.jtcproxy.stream.StreamReader;

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
        final var serverAddress = Inet4Address.getByName("127.0.0.1");
        final var socket = new Socket(serverAddress, server.getListeningPort());
        final var outputStream = socket.getOutputStream();
        final var inputStream = socket.getInputStream();

        final var inputMessage = new byte[] {
                0x01, // Version
                0x01, // ClientHello
                0x00, 0x00, // PayloadSize (16 Bit Unsigned int)
        };

        outputStream.write(inputMessage);
        outputStream.flush();

        final var buffer = new byte[1024];
        final var readBytes = inputStream.read(buffer);

        assertTrue(readBytes > 0);

        final var byteBuffer = Arrays.copyOfRange(buffer, 0, readBytes);
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

    @Test
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
        spawnRemoteConnection(socket);

        final var remoteSocket = new Socket("127.0.0.1", 3337);

        // Assert
        assertTrue(remoteSocket.isConnected());

        // Cleanup
        cancellationTokenSource.cancel();
        remoteSocket.close();
        socket.close();
        server.stopServer();
    }


    @Test
    public void shouldSendIncomingConnectionMessage() throws IOException, InterruptedException {
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
        spawnRemoteConnection(socket);

        final var remoteSocket = new Socket("127.0.0.1", 3337);
        assertTrue(remoteSocket.isConnected());

        final var remoteSocketId = receiveIncomingSocket(socket);

        // Assert
        assertNotNull(remoteSocketId);

        // Cleanup
        cancellationTokenSource.cancel();
        remoteSocket.close();
        socket.close();
        server.stopServer();
    }

    @Test
    public void shouldProxyDataInwards() throws IOException, InterruptedException {
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
        spawnRemoteConnection(socket);

        final var remoteSocket = new Socket("127.0.0.1", 3337);
        assertTrue(remoteSocket.isConnected());

        final var remoteSocketId = receiveIncomingSocket(socket);

        final var buffer = new byte[1024];
        final var random = new Random();
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = (byte) random.nextInt();
        }

        remoteSocket.getOutputStream().write(buffer);

        final var receivedBuffer = receiveBuffer(socket, remoteSocketId, buffer.length);

        // Assert
        assertNotNull(remoteSocketId);
        assertNotNull(receivedBuffer);
        assertTrue(Arrays.equals(buffer, receivedBuffer));

        // Cleanup
        cancellationTokenSource.cancel();
        remoteSocket.close();
        socket.close();
        server.stopServer();
    }

    private byte[] receiveBuffer(final Socket socket, final UUID remoteConnectionId, final int totalBytes) throws IOException {
        final var inputStream = socket.getInputStream();

        final var buffer = new byte[1024]; // Max receive, needs to be configured later.
        final var byteBuffer = ByteBuffer.allocate(4096);
        final var finalBuffer = ByteBuffer.allocate(totalBytes);
        final var streamReader = new StreamReader(byteBuffer);


        while (totalBytes > finalBuffer.position()) {
            final var bytesRead = inputStream.read(buffer, 0, buffer.length);
            assertTrue(bytesRead > 0);

            byteBuffer.put(buffer, 0, bytesRead);
            final var limit = byteBuffer.limit();
            final var position = byteBuffer.position();

            byteBuffer.flip();

            try {
                final var protocolVersion = ProtocolVersion.of(streamReader.readU8());
                final var messageType = MessageType.of(streamReader.readU8());
                final var connectionId = streamReader.readUuid();

                assertEquals(ProtocolVersion.Version1, protocolVersion);
                assertEquals(MessageType.DataMessage, messageType);
                assertTrue(remoteConnectionId.equals(connectionId));

                final var payloadSize = streamReader.readU16();
                final var payload = streamReader.readPayload(payloadSize);

                finalBuffer.put(payload);
                
            } catch (BufferIncompleteException e) {
                byteBuffer.limit(limit);
                byteBuffer.position(position);
            }
        }
        return finalBuffer.array();
    }

    private UUID receiveIncomingSocket(final Socket socket) throws IOException {
        final var inputStream = socket.getInputStream();

        final var buffer = new byte[1024];
        final var readBytes = inputStream.read(buffer);

        assertTrue(readBytes > 0);

        final var byteBuffer = Arrays.copyOfRange(buffer, 0, readBytes);
        final var expectedAnswer = new byte[] {
                0x01, // Version
                0x06, // IncomingSocket 
                0x00, 0x10, // PayloadSize (16 Bit Unsigned int)
        };

        final var messageHeader = Arrays.copyOfRange(byteBuffer, 0, 4);
        assertTrue(Arrays.equals(expectedAnswer, messageHeader));

        final var remainingPayload = Arrays.copyOfRange(byteBuffer, 4, byteBuffer.length);
        assertTrue(remainingPayload.length == 16);

        final var longBuffer = ByteBuffer.wrap(remainingPayload).asLongBuffer();
        return new UUID(longBuffer.get(), longBuffer.get());
    }

    private void spawnRemoteConnection(final Socket socket) throws IOException {
        final var outputStream = socket.getOutputStream();
        final var inputStream = socket.getInputStream();

        final var inputMessage = new byte[] {
                0x01,
                0x03, // Listen
                0x00, 0x00, // PayloadSize (16 Bit Unsigned int)
        };

        outputStream.write(inputMessage);
        outputStream.flush();

        final var buffer = new byte[1024];
        final var readBytes = inputStream.read(buffer);

        assertTrue(readBytes > 0);

        final var byteBuffer = Arrays.copyOfRange(buffer, 0, readBytes);
        final var expectedAnswer = new byte[] {
                0x01, // Version
                0x04, // ServerHello
                0x00, 0x02, // PayloadSize (16 Bit Unsigned int)
                0x0D, 0x09
        };

        assertTrue(readBytes > 0);
        assertTrue(readBytes == expectedAnswer.length);
        assertTrue(Arrays.equals(expectedAnswer, byteBuffer));
    }

    private void doHandshake(final Socket socket) throws IOException {
        final var outputStream = socket.getOutputStream();
        final var inputStream = socket.getInputStream();

        final var inputMessage = new byte[] {
                0x01, // Version
                0x01, // ClientHello
                0x00, 0x00, // PayloadSize (16 Bit Unsigned int)
        };

        outputStream.write(inputMessage);
        final var buffer = new byte[1024];
        final var readBytes = inputStream.read(buffer);

        assertTrue(readBytes > 0);

        final var byteBuffer = Arrays.copyOfRange(buffer, 0, readBytes);
        final var expectedAnswer = new byte[] {
                0x01, // Version
                0x02, // ServerHello
                0x00, 0x02, // PayloadSize (16 Bit Unsigned int)
                0x10, 0x00
        };

        // Assert
        assertTrue(readBytes > 0);
        assertTrue(Arrays.equals(expectedAnswer, byteBuffer));
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
