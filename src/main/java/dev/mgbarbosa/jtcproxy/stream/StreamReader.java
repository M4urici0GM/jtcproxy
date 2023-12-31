package dev.mgbarbosa.jtcproxy.stream;

import java.nio.ByteBuffer;
import java.util.UUID;

import org.apache.commons.lang3.NumberRange;

import dev.mgbarbosa.jtcproxy.exceptions.BufferIncompleteException;

public class StreamReader {

    private final ByteBuffer innerStream;

    public StreamReader(ByteBuffer stream) {
        this.innerStream = stream;
    }

    public byte[] readPayload(final int payloadSize) throws BufferIncompleteException {
        if (this.innerStream.remaining() < payloadSize) {
            throw new BufferIncompleteException();
        }

        final var byteBuffer = new byte[payloadSize];
        this.innerStream.get(byteBuffer);

        return byteBuffer;
    }

    public UUID readUuid() throws BufferIncompleteException {
        if (innerStream.remaining() < 16) {
            throw new BufferIncompleteException();
        }

        final var buffer = new byte[8];
        innerStream.get(buffer);
        final var mostSigBits = NumberBufferUtil.fromBytes(buffer, 8);

        innerStream.get(buffer);
        final var leastSigBits = NumberBufferUtil.fromBytes(buffer, 8);

        return new UUID(mostSigBits, leastSigBits);
    }

    public int readU16() throws BufferIncompleteException {
        if (innerStream.remaining() < 2) {
            throw new BufferIncompleteException();
        }

        return Short.toUnsignedInt(innerStream.getShort());
    }

    public int readU8() throws BufferIncompleteException {
        if (innerStream.remaining() < 1) {
            throw new BufferIncompleteException();
        }

        return Byte.toUnsignedInt(innerStream.get());
    }
}
