
package dev.mgbarbosa.jtcproxy.message;

import java.nio.ByteBuffer;

import dev.mgbarbosa.jtcproxy.buffers.NumberBufferUtil;
import dev.mgbarbosa.jtcproxy.exceptions.BufferIncompleteException;
import dev.mgbarbosa.jtcproxy.protocol.Message;
import dev.mgbarbosa.jtcproxy.protocol.MessageParser;
import dev.mgbarbosa.jtcproxy.protocol.MessageParserException;
import dev.mgbarbosa.jtcproxy.protocol.MessageType;
import dev.mgbarbosa.jtcproxy.protocol.ProtocolVersion;

/**
 * DataMessageParser
 */
public class DataMessageParser implements MessageParser {

    private final ByteBuffer innerStream;

    public DataMessageParser(final ByteBuffer innerStream) {
        this.innerStream = innerStream;
    }

    @Override
    public Message parseFrom(ProtocolVersion version) throws BufferIncompleteException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'parseFrom'");
    }

    @Override
    public void write(Message message) {
        if (!(message instanceof DataMessage dataMessage)) {
            throw new MessageParserException("Invalid argument");
        }
        final var connectionId = dataMessage.connectionId();

        innerStream.put((byte) ProtocolVersion.Version1.getRaw());
        innerStream.put((byte) MessageType.DataMessage.getRaw());
        innerStream.put(NumberBufferUtil.toBytes(connectionId.getMostSignificantBits(), 8));
        innerStream.put(NumberBufferUtil.toBytes(connectionId.getLeastSignificantBits(), 8));
        innerStream.putShort((short) (dataMessage.payloadSize() & 0xFFFF));
        innerStream.put(dataMessage.payload());
    }
}
