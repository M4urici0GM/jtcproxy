package dev.mgbarbosa.jtcproxy.stream;

import java.nio.ByteBuffer;

import dev.mgbarbosa.jtcproxy.exceptions.BufferIncompleteException;

public class StreamReader {

    private final ByteBuffer innerStream;

    public StreamReader(ByteBuffer stream) {
        this.innerStream = stream;
    }

    public byte[] readPayload(final int payloadSize) {
        if (this.innerStream.remaining() < payloadSize) {
            throw new BufferIncompleteException();
        }

        final var byteBuffer = new byte[payloadSize];
        this.innerStream.get(byteBuffer);

        return byteBuffer;
    }

    public int readU16() {
        if (innerStream.remaining() < 2) {
            throw new BufferIncompleteException();
        }

        return Short.toUnsignedInt(innerStream.getShort());
    }

    public int readU8() {
        if (innerStream.remaining() < 1) {
            throw new BufferIncompleteException();
        }

        return Byte.toUnsignedInt(innerStream.get());
    }
}
