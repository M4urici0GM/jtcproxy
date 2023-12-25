package dev.mgbarbosa.jtcproxy.protocol;

import dev.mgbarbosa.jtcproxy.exceptions.BufferIncompleteException;

public interface MessageParser {
    Message parseFrom(final ProtocolVersion version) throws BufferIncompleteException;

    void write(final Message message);
}
