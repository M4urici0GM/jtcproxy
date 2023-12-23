package dev.mgbarbosa.jtcproxy.message;

import java.nio.ByteBuffer;

import dev.mgbarbosa.jtcproxy.exceptions.BufferIncompleteException;
import dev.mgbarbosa.jtcproxy.protocol.Message;
import dev.mgbarbosa.jtcproxy.protocol.MessageParser;
import dev.mgbarbosa.jtcproxy.protocol.MessageParserException;
import dev.mgbarbosa.jtcproxy.protocol.MessageType;
import dev.mgbarbosa.jtcproxy.protocol.ProtocolVersion;
import dev.mgbarbosa.jtcproxy.stream.StreamReader;

public class ServerHelloMessageParser implements MessageParser {

    private final ByteBuffer buffer;

    public ServerHelloMessageParser(final ByteBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public Message parseFrom(ProtocolVersion version) {
        final var innerStream = new StreamReader(buffer);
        final var payloadSize = innerStream.readU16();
        if (payloadSize < 2) {
            throw new BufferIncompleteException();
        }

        final var maxReceive = innerStream.readU16();
        return new ServerHelloMessage(maxReceive);
    }

    @Override
    public void write(Message message) {
        if (!(message instanceof ServerHelloMessage helloMessage)) {
            throw new MessageParserException("Invalid argument type");
        }

        buffer.put((byte) ProtocolVersion.Version1.getRaw());
        buffer.put((byte) MessageType.ServerHello.getRaw());
        buffer.putShort((short) 2);
        buffer.putShort((short) helloMessage.getMaxReceive());
    }
}
