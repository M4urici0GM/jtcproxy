
package dev.mgbarbosa.jtcproxy.message;

import static dev.mgbarbosa.jtcproxy.Constants.U16_SIZE;
import static dev.mgbarbosa.jtcproxy.Constants.U8_SIZE;

import java.util.UUID;

import dev.mgbarbosa.jtcproxy.protocol.Message;
import dev.mgbarbosa.jtcproxy.protocol.MessageType;

/**
 * DataMessage
 */
public class DataMessage implements Message {

    private final byte[] payload;
    private final UUID connectionId;

    public DataMessage(final byte[] payload, final UUID connectionId) {
        this.payload = payload;
        this.connectionId = connectionId;
    }

    public UUID connectionId() {
        return this.connectionId;
    }

    public int payloadSize() {
        return payload.length;
    }

    public byte[] payload() {
        return this.payload;
    }

    @Override
    public MessageType type() {
        return MessageType.DataMessage;
    }

    @Override
    public int getSize() {
        return U8_SIZE + // Version
                U8_SIZE + // Message Type
                16 +
                U16_SIZE + // Payload size
                (short) (payload.length & 0xFFFF); // Total Payload size
    }

    
}
