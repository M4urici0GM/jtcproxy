
package dev.mgbarbosa.jtcproxy.message;

import java.nio.ByteBuffer;

import org.apache.commons.lang3.NotImplementedException;

import dev.mgbarbosa.jtcproxy.protocol.Message;
import dev.mgbarbosa.jtcproxy.protocol.MessageParser;
import dev.mgbarbosa.jtcproxy.protocol.MessageParserException;
import dev.mgbarbosa.jtcproxy.protocol.MessageType;
import dev.mgbarbosa.jtcproxy.protocol.ProtocolVersion;

public class ListenMessageParser implements MessageParser {

    private final ByteBuffer byteBuffer;

    public ListenMessageParser(final ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    @Override
    public Message parseFrom(ProtocolVersion version) {
        return new ListenMessage();
    }

    @Override
    public void write(final Message message) {
        if (!(message instanceof ListenMessage listenMessage)) {
            throw new MessageParserException("Invalid argument");
        }

        byteBuffer.put((byte) ProtocolVersion.Version1.getRaw());
        byteBuffer.put((byte) MessageType.Listen.getRaw());
        byteBuffer.putShort((short) 0);
    }
}
