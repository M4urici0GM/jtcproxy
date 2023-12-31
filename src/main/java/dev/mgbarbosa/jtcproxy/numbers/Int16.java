
package dev.mgbarbosa.jtcproxy.numbers;

import dev.mgbarbosa.jtcproxy.buffers.NumberBufferUtil;

public record Int16(Short value) {

    public static Integer SIZE = 2;

    public byte[] toBytes() {
        return NumberBufferUtil.toBytes(value, SIZE);
    }

    public static Int64 fromBytes(final byte[] buffer) {
        return new Int64(NumberBufferUtil.fromBytes(buffer, SIZE));
    }
}
