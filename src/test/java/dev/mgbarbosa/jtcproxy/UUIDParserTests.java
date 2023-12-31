package dev.mgbarbosa.jtcproxy;

import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.Test;

import dev.mgbarbosa.jtcproxy.buffers.NumberBufferUtil;
import dev.mgbarbosa.jtcproxy.exceptions.BufferIncompleteException;
import dev.mgbarbosa.jtcproxy.stream.StreamReader;

/**
 * UUIDParserTests
 */
public class UUIDParserTests {

    @Test
    public void shouldBeAbleToDeserializeUuid() throws Exception {
        // Arrange
        final var expectedUuid = randomUUID();
        final var buffer = new byte[16];
        final var byteBuffer = ByteBuffer.wrap(buffer);

        byteBuffer.put(NumberBufferUtil.toBytes(expectedUuid.getMostSignificantBits(), 8));
        byteBuffer.put(NumberBufferUtil.toBytes(expectedUuid.getLeastSignificantBits(), 8));
        byteBuffer.flip();
        
        // Act
        final var reader = new StreamReader(byteBuffer);
        final var result = reader.readUuid();

        // Assert
        assertTrue(expectedUuid.equals(result));
    }

    @Test
    public void shouldThrowBufferIncompleteException_whenBufferDoesntHaveEnoughtBytes() {
        // Arrange
        final var buffer = ByteBuffer.allocate(64);
        buffer.putLong(10);
        buffer.flip();

        final var streamReader = new StreamReader(buffer);

        // Assert
        assertThrows(BufferIncompleteException.class, () -> streamReader.readUuid());
    }
}
