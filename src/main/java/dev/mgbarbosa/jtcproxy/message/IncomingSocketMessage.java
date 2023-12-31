
package dev.mgbarbosa.jtcproxy.message;

import static dev.mgbarbosa.jtcproxy.Constants.*;

import java.util.UUID;

import dev.mgbarbosa.jtcproxy.protocol.Message;
import dev.mgbarbosa.jtcproxy.protocol.MessageType;

/**
 * IncomingSocketMessage
 */
public class IncomingSocketMessage implements Message {

    private final UUID connectionId;
    
    public IncomingSocketMessage(UUID connectionId) {
        this.connectionId = connectionId;
    }

    public UUID connectionId() {
        return this.connectionId;
    }

    @Override
    public MessageType type() { return MessageType.IncomingSocket; }

    @Override
    public int getSize() {
        return U8_SIZE + // Version
                U8_SIZE + // Message Type
                U16_SIZE + // PayloadSize
                16; // UUID
    }
}
