package dev.mgbarbosa.jtcproxy.protocol;

public interface MessageParser {
    Message parseFrom(final ProtocolVersion version);

    void write(final Message message);
}
