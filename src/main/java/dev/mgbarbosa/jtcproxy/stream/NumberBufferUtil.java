
package dev.mgbarbosa.jtcproxy.stream;

import java.security.InvalidParameterException;

/**
 * NumberBufferUtil
 */
public class NumberBufferUtil {

    /**
     * Converts an number to a bytearray.
     * Notes that this function expects a long, but you can convert any number.
     * Just convert it to a long first.
     * 
     * @param value    The number to be parsed.
     * @param typeSize How many bytes the type occupies.
     **/
    public static byte[] toBytes(long value, int typeSize) {
        final var buffer = new byte[typeSize];
        for (int i = typeSize - 1; i >= 0; i--) {
            buffer[i] = (byte) (value & 0xFF);
            value >>>= 8;
        }

        return buffer;
    }

    /**
     * Converts given buffer to a number.
     * This mehtod is able to convert to any type of number, returning
     * by default the biggest type, to prevent overflow.
     * After convertion you may demote the type to the required type.
     **/
    public static long fromBytes(byte[] buffer, int typeSize) {
        if (buffer.length < typeSize) {
            throw new InvalidParameterException(
                    String.format("Expected at least %s bytes, received %s.", typeSize, buffer.length));
        }

        long value = 0x00;
        for (int i = 0; i < typeSize; i++) {
            value = (value << 8) | (buffer[i] & 0xFF);
        }

        return value;
    }
}
