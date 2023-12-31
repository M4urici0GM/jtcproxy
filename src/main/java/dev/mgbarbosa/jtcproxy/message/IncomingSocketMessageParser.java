
package dev.mgbarbosa.jtcproxy.message;

import static dev.mgbarbosa.jtcproxy.Constants.U16_SIZE;

import java.nio.ByteBuffer;

import dev.mgbarbosa.jtcproxy.buffers.NumberBufferUtil;
import dev.mgbarbosa.jtcproxy.exceptions.BufferIncompleteException;
import dev.mgbarbosa.jtcproxy.protocol.Message;
import dev.mgbarbosa.jtcproxy.protocol.MessageParser;
import dev.mgbarbosa.jtcproxy.protocol.MessageParserException;
import dev.mgbarbosa.jtcproxy.protocol.MessageType;
import dev.mgbarbosa.jtcproxy.protocol.ProtocolVersion;

/**
 * IncomingSocketMessageParser
 */
public class IncomingSocketMessageParser implements MessageParser {

    private final ByteBuffer innerStream;

    public IncomingSocketMessageParser(final ByteBuffer buffer) {
        this.innerStream = buffer;
    }

    @Override
    public Message parseFrom(ProtocolVersion version) throws BufferIncompleteException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'parseFrom'");
    }

    @Override
    public void write(Message message) {
        if (!(message instanceof IncomingSocketMessage incomingMessage)) {
            throw new MessageParserException("Invalid argument");
        }

        final var connectionId = incomingMessage.connectionId();

        innerStream.put((byte) ProtocolVersion.Version1.getRaw());
        innerStream.put((byte) MessageType.IncomingSocket.getRaw());
        innerStream.putShort((short) 16);
        innerStream.put(NumberBufferUtil.toBytes(connectionId.getMostSignificantBits(), 8));
        innerStream.put(NumberBufferUtil.toBytes(connectionId.getLeastSignificantBits(), 8));
    }

}
