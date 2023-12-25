
package dev.mgbarbosa.jtcproxy.message;

import static dev.mgbarbosa.jtcproxy.Constants.U16_SIZE;

import java.nio.ByteBuffer;

import dev.mgbarbosa.jtcproxy.protocol.Message;
import dev.mgbarbosa.jtcproxy.protocol.MessageParser;
import dev.mgbarbosa.jtcproxy.protocol.MessageParserException;
import dev.mgbarbosa.jtcproxy.protocol.MessageType;
import dev.mgbarbosa.jtcproxy.protocol.ProtocolVersion;
import lombok.RequiredArgsConstructor;

/**
 * ListenAckMessageParser
 **/
@RequiredArgsConstructor
public class ListenAckMessageParser implements MessageParser {

    private final ByteBuffer byteBuffer;

    @Override
    public Message parseFrom(ProtocolVersion version) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'parseFrom'");
    }

    @Override
    public void write(Message message) {
        if (!(message instanceof ListenAckMessage listenMessage)) {
            throw new MessageParserException("Invalid argument");
        }

        byteBuffer.put((byte) ProtocolVersion.Version1.getRaw());
        byteBuffer.put((byte) MessageType.Listen.getRaw());
        byteBuffer.putShort((short) U16_SIZE);
        byteBuffer.putShort((short) listenMessage.getListeningPort());
    }
}
