package dev.mgbarbosa.jtcproxy.message;

import dev.mgbarbosa.jtcproxy.protocol.Message;
import dev.mgbarbosa.jtcproxy.protocol.MessageType;
import dev.mgbarbosa.jtcproxy.protocol.ProtocolVersion;

public class ClientHelloMessage implements Message {
    @Override
    public MessageType type() {
        return MessageType.ClientHello;
    }

    @Override
    public ProtocolVersion version() {
        return ProtocolVersion.Version1;
    }

    @Override
    public int getSize() {
        return 4; // ProtocolVersion + MessageType + PayloadSize
    }
}
