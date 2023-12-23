package dev.mgbarbosa.jtcproxy.protocol;

public interface Message {
    MessageType type();

    ProtocolVersion version();

    int getSize();
}
