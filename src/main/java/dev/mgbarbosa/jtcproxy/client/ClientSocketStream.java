package dev.mgbarbosa.jtcproxy.client;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CompletableFuture;

import dev.mgbarbosa.jtcproxy.cancellation.CancellationToken;
import dev.mgbarbosa.jtcproxy.exceptions.BufferIncompleteException;
import dev.mgbarbosa.jtcproxy.message.ClientHelloMessage;
import dev.mgbarbosa.jtcproxy.message.ClientHelloMessageParser;
import dev.mgbarbosa.jtcproxy.message.ServerHelloMessage;
import dev.mgbarbosa.jtcproxy.message.ServerHelloMessageParser;
import dev.mgbarbosa.jtcproxy.protocol.Message;
import dev.mgbarbosa.jtcproxy.protocol.MessageParser;
import dev.mgbarbosa.jtcproxy.protocol.MessageType;
import dev.mgbarbosa.jtcproxy.protocol.ProtocolVersion;
import dev.mgbarbosa.jtcproxy.stream.StreamReader;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ClientSocketStream implements Closeable {
    private static final Integer END_OF_STREAM = -1;

    private final ByteBuffer clientBuffer;
    private final SocketChannel client;

    @Builder.Default
    private ClientStatus status = ClientStatus.HANDSHAKE;

    public void start(final CancellationToken cancellationToken) throws Exception {
        while (!cancellationToken.isCancellationRequested()) {
            final var readBytes = CompletableFuture.supplyAsync(() -> {
                try {
                    while (true) {
                        final var bytes = this.client.read(this.clientBuffer);
                        if (bytes == 0) {
                            if (cancellationToken.isCancellationRequested()) {
                                break;
                            }
                        }

                        if (END_OF_STREAM.equals(bytes)) {
                            throw new RuntimeException("Reached End of Stream.");
                        }

                        return bytes;
                    }
                } catch (Exception ex) {
                    log.error("Error when reading from socket", ex);
                }

                return null;
            }).get();

            if (readBytes == null || readBytes <= 0) {
                log.debug("received 0 bytes from socket. closing socket.");
                break;
            }

            clientBuffer.flip();
            final var currentPosition = this.clientBuffer.position();
            final var streamReader = new StreamReader(this.clientBuffer);
            try {
                final var protocolVersion = ProtocolVersion.of(streamReader.readU8());
                final var messageType = MessageType.of(streamReader.readU8());

                final var messageParser = getParser(messageType, clientBuffer);
                final var message = messageParser.parseFrom(protocolVersion);

                handleMessage(message);
            } catch (BufferIncompleteException ex) {
                log.debug("Buffer is not complete yet.");
                clientBuffer.position(currentPosition);
            } catch (Exception ex) {
                log.error("received unexpected exception.", ex);
                break;
            }
        }
    }

    private void handleMessage(Message message) throws IOException {
        if (message instanceof ClientHelloMessage) {
            log.info("received ClientHello message..");
            final var answerMessage = new ServerHelloMessage(4096);
            final var buffer = ByteBuffer.allocate(8);

            new ServerHelloMessageParser(buffer).write(answerMessage);
            buffer.flip();

            while (this.client.write(buffer) != 0) {}

            this.clientBuffer.compact();
            this.status = ClientStatus.CONNECTED;
            return;
        }
    }

    private MessageParser getParser(MessageType messageType, ByteBuffer buffer) {
        return switch (messageType) {
            case MessageType.ClientHello -> new ClientHelloMessageParser(buffer);
            case MessageType.ServerHello -> throw new RuntimeException("Client cannot send ServerHello");
        };
    }

    @Override
    public void close() throws IOException {
        this.client.socket().close();
        this.client.close();
    }
}
