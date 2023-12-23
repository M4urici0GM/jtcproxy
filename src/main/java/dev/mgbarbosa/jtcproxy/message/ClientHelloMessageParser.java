package dev.mgbarbosa.jtcproxy.message;

import java.nio.ByteBuffer;

import dev.mgbarbosa.jtcproxy.protocol.Message;
import dev.mgbarbosa.jtcproxy.protocol.MessageParser;
import dev.mgbarbosa.jtcproxy.protocol.MessageParserException;
import dev.mgbarbosa.jtcproxy.protocol.MessageType;
import dev.mgbarbosa.jtcproxy.protocol.ProtocolVersion;
import dev.mgbarbosa.jtcproxy.stream.StreamReader;

public class ClientHelloMessageParser implements MessageParser {

    private final ByteBuffer buffer;

    public ClientHelloMessageParser(final ByteBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public Message parseFrom(final ProtocolVersion version) {
        final var streamReader = new StreamReader(buffer);
        final var payloadSize = streamReader.readU16();

        if (payloadSize > 0) {
            throw new MessageParserException("ClientHello should not have any payload.");
        }

        return new ClientHelloMessage();
    }

    @Override
    public void write(final Message message) {
        if (!(message instanceof ClientHelloMessage clientHello)) {
            throw new MessageParserException("Invalid argument.");
        }

        buffer.putShort((short) ProtocolVersion.Version1.getRaw());
        buffer.putShort((short) MessageType.ClientHello.getRaw());
        buffer.putShort((short) 0);
    }
}
