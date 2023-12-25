package dev.mgbarbosa.jtcproxy.client;

import static java.util.Objects.isNull;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.NotImplementedException;

import dev.mgbarbosa.jtcproxy.cancellation.CancellationToken;
import dev.mgbarbosa.jtcproxy.exceptions.BufferIncompleteException;
import dev.mgbarbosa.jtcproxy.message.ClientHelloMessage;
import dev.mgbarbosa.jtcproxy.message.ClientHelloMessageParser;
import dev.mgbarbosa.jtcproxy.message.ListenAckMessageParser;
import dev.mgbarbosa.jtcproxy.message.ListenMessage;
import dev.mgbarbosa.jtcproxy.message.ListenMessageParser;
import dev.mgbarbosa.jtcproxy.message.ServerHelloMessage;
import dev.mgbarbosa.jtcproxy.message.ServerHelloMessageParser;
import dev.mgbarbosa.jtcproxy.protocol.Message;
import dev.mgbarbosa.jtcproxy.protocol.MessageParser;
import dev.mgbarbosa.jtcproxy.protocol.MessageType;
import dev.mgbarbosa.jtcproxy.protocol.ProtocolVersion;
import dev.mgbarbosa.jtcproxy.server.PortProvider;
import dev.mgbarbosa.jtcproxy.stream.StreamReader;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ClientSocketStream implements Closeable {
    private static final int MAX_RECEIVE = 4096;

    private static final Integer END_OF_STREAM = -1;

    private final ByteBuffer clientBuffer;
    private final SocketChannel client;
    private final PortProvider portProvider;

    @Builder.Default
    private ClientStatus status = ClientStatus.HANDSHAKE;

    public void start(final CancellationToken cancellationToken) throws Exception {
        while (!cancellationToken.isCancellationRequested()) {
            final var readBytes = readFromSocket(cancellationToken);
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

                handleMessage(message, cancellationToken);
            } catch (BufferIncompleteException ex) {
                log.debug("Buffer is not complete yet.");
                clientBuffer.position(currentPosition);
            } catch (Exception ex) {
                log.error("received unexpected exception.", ex);
                break;
            }
        }
    }

    @Override
    public void close() throws IOException {
        this.client.socket().close();
        this.client.close();
    }

    private Integer readFromSocket(final CancellationToken cancellationToken) throws IOException, InterruptedException {
        while (true) {
            final var bytes = this.client.read(this.clientBuffer);
            if (bytes == 0) {
                Thread.yield();
                cancellationToken.throwIfCancellationRequested();
                continue;
            }

            if (END_OF_STREAM.equals(bytes)) {
                throw new RuntimeException("Reached End of Stream.");
            }

            return bytes;
        }
    }

    private void handleMessage(Message message, CancellationToken cancellationToken)
            throws IOException, InterruptedException, ExecutionException {
        if (message instanceof ClientHelloMessage) {
            log.info("received ClientHello message..");
            final var answerMessage = new ServerHelloMessage(4096);
            final var buffer = ByteBuffer.allocate(8);
            final var messageParser = new ServerHelloMessageParser(buffer);

            messageParser.write(answerMessage);
            buffer.flip();

            while (this.client.write(buffer) != 0) { }

            this.clientBuffer.compact();
            this.status = ClientStatus.CONNECTED;
            return;
        }

        if (message instanceof ListenMessage) {
            log.info("received listen message");
            final var reservedPort = portProvider.get();
            final var executor = Executors.newVirtualThreadPerTaskExecutor();

            final var address = new InetSocketAddress("0.0.0.0", reservedPort.getPort());
            final var server = new ServerSocket();
            server.bind(address);

            final var semaphore = new Semaphore(100);
            final var clientQueue = new ArrayBlockingQueue<byte[]>(10);
            final var connectionMap = new ConcurrentHashMap<UUID, ArrayBlockingQueue<byte[]>>();

            executor.submit(() -> {
                // Accepts connection
                while (!cancellationToken.isCancellationRequested()) {
                    try {
                        semaphore.acquire();
                        final var acceptedSocket = server.accept();
                        final var inputStream = acceptedSocket.getInputStream();
                        final var outputStream = acceptedSocket.getOutputStream();

                        final var connectionId = UUID.randomUUID();
                        final var connectionQueue = new ArrayBlockingQueue<byte[]>(10);

                        connectionMap.put(connectionId, connectionQueue);

                        executor.submit(() -> {
                            final var future1 = CompletableFuture.runAsync(() -> {
                                try {
                                    while (true) {
                                        final byte[] polledObject = connectionQueue.poll();
                                        if (isNull(polledObject)) {
                                            continue;
                                        }

                                        // TODO: handle error.
                                        outputStream.write(polledObject);
                                    }
                                } catch (IOException ex) {
                                    // TODO: handle error.
                                }
                            });

                            final var future = CompletableFuture.runAsync(() -> {
                                var channel = Channels.newChannel(inputStream);
                                var buffer = ByteBuffer.allocateDirect(1024 * 1024 * 4); // 4MB

                                try {
                                    int bytesRead = 0;
                                    while (true) {
                                        bytesRead += channel.read(buffer);
                                        buffer.flip();

                                        var byteBuffer = new byte[bytesRead];
                                        buffer.get(byteBuffer);

                                        try {
                                            clientQueue.offer(byteBuffer, 1000, TimeUnit.MILLISECONDS);
                                            buffer.compact();
                                            bytesRead = 0;
                                        } catch (InterruptedException ex) {
                                            buffer.flip();
                                        } // This is so we can try again later.
                                    }
                                } catch (IOException ex) {
                                    // TODO: handle error.
                                }
                            });

                            CompletableFuture.anyOf(future1, future).join();
                            // TODO: check why the failure, and handle it accordingly
                            semaphore.release();
                        });

                    } catch (IOException ex) {
                    } catch (InterruptedException ex) {
                    }
                }
            });

            executor.submit(() -> {
                try {
                    while (!cancellationToken.isCancellationRequested()) {
                        final var polledObject = clientQueue.poll();
                        if (isNull(polledObject)) {
                            Thread.yield();
                            continue;
                        }

                        final var byteBuffer = ByteBuffer.allocate(1024 * 1024 * 4);
                        byteBuffer.put((byte) ProtocolVersion.Version1.getRaw());
                        byteBuffer.put((byte) MessageType.DataMessage.getRaw());
                        byteBuffer.putShort((short) polledObject.length);
                        byteBuffer.put(polledObject, 0, polledObject.length);
                        byteBuffer.flip();

                        while ((this.client.write(byteBuffer)) != END_OF_STREAM) {
                            Thread.yield();
                            cancellationToken.throwIfCancellationRequested();
                        }
                    }
                } catch (Exception ex) {
                    log.error("failed when writing to client.", ex);
                }
            });

            final var streamReader = new StreamReader(this.clientBuffer);
            while (!cancellationToken.isCancellationRequested()) {
                final var currentPosition = clientBuffer.position();
                try {
                    final var messageHeader = MessageHeader.readMessageHeader(this.clientBuffer);
                    switch (messageHeader.getType()) {
                        case DataMessage:
                            final var payloadLength = streamReader.readU16();
                            if (payloadLength > MAX_RECEIVE) {
                                throw new InvalidMessageException("Payload cannot exceed max_receive");
                            }

                            final var connectionId = streamReader.readUuid();
                            if (!connectionMap.containsKey(connectionId)) {
                                throw new RuntimeException();
                            }

                            if (payloadLength > clientBuffer.remaining()) {
                                log.debug("not received entire payload yet.");
                                clientBuffer.position(currentPosition);
                                continue;
                            }

                            final var buffer = new byte[payloadLength];
                            clientBuffer.get(buffer);

                            final var connectionQueue = connectionMap.get(connectionId);
                            final var atomicInteger = new AtomicInteger(0);
                            while (true) {
                                try {
                                    connectionQueue.offer(buffer, 200, TimeUnit.MILLISECONDS);
                                    break;
                                } catch (InterruptedException ex) {
                                    atomicInteger.getAndIncrement();
                                    if (atomicInteger.get() > 3) {
                                        throw new InterruptedException();
                                    }
                                }
                            }

                            clientBuffer.compact();

                        default:
                            throw new InvalidMessageException();
                    }
                } catch (InvalidMessageException ex) {
                    log.error("Got invalid message type.");
                    break;
                } catch (BufferIncompleteException ex) {
                    clientBuffer.position(currentPosition);
                    log.debug("buffer not ready yet.");
                }
            }

            server.close();
            log.debug("shutting executor down..");
            executor.shutdown();

            final var result = executor.awaitTermination(1000, TimeUnit.MILLISECONDS);
            if (!result) {
                log.warn("failed to shutdown executor. killing it.");
                executor.shutdownNow();

                final var killResult = executor.awaitTermination(2000, TimeUnit.MILLISECONDS);
                if (!killResult) {
                    log.error("error when trying to kill executor");
                    throw new ExecutionException("error when trying to kill task executor", null);
                }
            }
        }
    }

    private MessageParser getParser(MessageType messageType, ByteBuffer buffer) {
        return switch (messageType) {
            case MessageType.ClientHello -> new ClientHelloMessageParser(buffer);
            case MessageType.Listen -> new ListenMessageParser(buffer);
            case MessageType.ListenAck -> new ListenAckMessageParser(buffer);
            case MessageType.DataMessage -> throw new NotImplementedException();
            case MessageType.ServerHello -> throw new RuntimeException("Client cannot send ServerHello");
            default -> throw new RuntimeException("Message type not found");
        };
    }

    static class InvalidMessageException extends Exception {
        public InvalidMessageException(final String message) {
            super(message);
        }

        public InvalidMessageException() {
            this("Invalid message type");
        }
    }

    @Getter
    @RequiredArgsConstructor
    static class MessageHeader {
        private final ProtocolVersion version;
        private final MessageType type;

        public static MessageHeader readMessageHeader(ByteBuffer buffer) throws BufferIncompleteException {
            final var streamReader = new StreamReader(buffer);
            final var rawProtocolVersion = streamReader.readU8();
            final var rawMessageType = streamReader.readU16();

            return new MessageHeader(ProtocolVersion.of(rawProtocolVersion), MessageType.of(rawMessageType));
        }
    }
}
