package dev.mgbarbosa.jtcproxy.numbers;

import dev.mgbarbosa.jtcproxy.buffers.NumberBufferUtil;

public record Int64(Long value) {

    public static Integer SIZE = 8;

    public byte[] toBytes() {
        return NumberBufferUtil.toBytes(value, SIZE);
    }

    public static Int64 fromBytes(final byte[] buffer) {
        return new Int64(NumberBufferUtil.fromBytes(buffer, SIZE));
    }
}
