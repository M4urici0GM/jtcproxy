package dev.mgbarbosa.jtcproxy.protocol;

public interface Message {
    MessageType type();

    default ProtocolVersion version() {
        return ProtocolVersion.Version1;
    }

    int getSize();
}
