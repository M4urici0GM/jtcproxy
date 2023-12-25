package dev.mgbarbosa.jtcproxy.message;

import static dev.mgbarbosa.jtcproxy.Constants.U16_SIZE;
import static dev.mgbarbosa.jtcproxy.Constants.U8_SIZE;

import dev.mgbarbosa.jtcproxy.protocol.Message;
import dev.mgbarbosa.jtcproxy.protocol.MessageType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ListenAckMessage implements Message {

    private final int listeningPort;

    @Override
    public MessageType type() {
        return MessageType.ListenAck;
    }

    @Override
    public int getSize() {
        return U8_SIZE + // Version
                U8_SIZE + // MessageType
                U16_SIZE + // PayloadSize
                U16_SIZE; // ListeningPort
    }

}
