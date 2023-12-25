package dev.mgbarbosa.jtcproxy;

import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.Test;

import dev.mgbarbosa.jtcproxy.exceptions.BufferIncompleteException;
import dev.mgbarbosa.jtcproxy.stream.StreamReader;

/**
 * UUIDParserTests
 */
public class UUIDParserTests {

    @Test
    public void shouldBeAbleToSerializeUUID() {
        // Arrange
        final var uuid = randomUUID();

        // Act
        final var result = StreamReader.convertUuid(uuid);

        // Assert
        assertNotNull(result);
        assertTrue(result.length == 16);
    }

    @Test
    public void shouldBeAbleToDeserializeUuid() throws Exception {
        // Arrange
        final var expectedUuid = randomUUID();
        final var outputStream = new ByteArrayOutputStream(16);
        outputStream.writeBytes(Longs)
        outputStream.write((byte) expectedUuid.getLeastSignificantBits());
        outputStream.flush();

        final var byteBuffer = ByteBuffer.wrap(outputStream.toByteArray());
        
        // Act
        final var reader = new StreamReader(byteBuffer);
        final var result = reader.readUuid();

        // Assert
        assertTrue(expectedUuid == result);
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
