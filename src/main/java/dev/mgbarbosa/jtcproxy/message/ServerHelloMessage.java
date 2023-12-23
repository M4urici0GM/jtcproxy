package dev.mgbarbosa.jtcproxy.message;

import dev.mgbarbosa.jtcproxy.protocol.Message;
import dev.mgbarbosa.jtcproxy.protocol.MessageType;
import dev.mgbarbosa.jtcproxy.protocol.ProtocolVersion;
import lombok.Getter;

@Getter
public class ServerHelloMessage implements Message {

    private final int maxReceive;

    public ServerHelloMessage(final int maxReceive) {
        this.maxReceive = maxReceive;
    }

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
        return 6; // ProtocolVersion(1) + MessageType(1) + PayloadSize(2) + Payload(2)
    }
}
