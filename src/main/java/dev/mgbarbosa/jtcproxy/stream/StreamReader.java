package dev.mgbarbosa.jtcproxy.stream;

import java.nio.ByteBuffer;
import java.util.UUID;

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

        final var buffer = new byte[16];
        innerStream.get(buffer, 0, buffer.length);

        return UUID.nameUUIDFromBytes(buffer);
    }

    public static byte[] convertUuid(final UUID uuid) {
        final var buffer = new byte[16];
        return ByteBuffer.wrap(buffer)
            .putLong(uuid.getMostSignificantBits())
            .putLong(uuid.getLeastSignificantBits())
            .array();
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
