

package dev.mgbarbosa.jtcproxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

import dev.mgbarbosa.jtcproxy.buffers.NumberBufferUtil;


/**
 * BufferUtilsTest
 */
public class BufferUtilsTest {

    @Test
    public void shouldBeAbleToSerializeAndDeserializeLong() {
        // Arrange
        final var random = new Random();
        final var expectedValue = (Long) random.nextLong();

        // Act
        final var buffer = NumberBufferUtil.toBytes(expectedValue, 8);
        final var deserializedValue = (Long) NumberBufferUtil.fromBytes(buffer, 8);

        // Assert
        assertEquals(expectedValue, deserializedValue);
    }


    @Test
    public void shouldBeAbleToSerializeAndDeserializeInt32() {
        // Arrange
        final var random = new Random();
        final var expectedValue = (int) random.nextInt();

        // Act
        final var buffer = NumberBufferUtil.toBytes((long) expectedValue, 4);
        final var value = ((int) NumberBufferUtil.fromBytes(buffer, 4));

        // Assert
        assertTrue(expectedValue == value);
    }

    @Test
    public void shouldBeAbleToSerializeAndDeserializeInt16() {
        // Arrange
        final var random = new Random();
        final var expectedValue = (short) (random.nextInt() & 0xFFFF);

        // Act
        final var buffer = NumberBufferUtil.toBytes((long) expectedValue, 2);
        final var value = (short) (NumberBufferUtil.fromBytes(buffer, 2) & 0xFFFF);

        // Assert
        assertTrue(expectedValue == value);
    }

    @Test
    public void shouldBeAbleToSerializeAndDeserializeInt8() {
        // Arrange
        final var random = new Random();
        final var expectedValue = (short) (random.nextInt() & 0xFF);

        // Act
        final var buffer = NumberBufferUtil.toBytes((long) expectedValue, 1);
        final var value = (short) (NumberBufferUtil.fromBytes(buffer, 1) & 0xFF);

        // Assert
        assertTrue(expectedValue == value);
    }
}
