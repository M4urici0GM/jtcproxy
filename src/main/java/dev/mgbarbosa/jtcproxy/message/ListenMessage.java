
package dev.mgbarbosa.jtcproxy.message;

import dev.mgbarbosa.jtcproxy.protocol.Message;
import dev.mgbarbosa.jtcproxy.protocol.MessageType;
import dev.mgbarbosa.jtcproxy.protocol.ProtocolVersion;

import static dev.mgbarbosa.jtcproxy.Constants.*;

public class ListenMessage implements Message {

    @Override
    public MessageType type() {
        return MessageType.Listen;
    }

    @Override
    public ProtocolVersion version() {
        return ProtocolVersion.Version1;
    }

    @Override
    public int getSize() {
        return U8_SIZE + // Version
                U8_SIZE + // Message Type
                U16_SIZE; // PayloadSize
    }
}
