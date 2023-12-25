package dev.mgbarbosa.jtcproxy.protocol;

import dev.mgbarbosa.jtcproxy.exceptions.InvalidMessageType;

public enum MessageType {
    ClientHello,
    ServerHello,
    Listen,
    ListenAck,
    DataMessage;

    public static MessageType of(int value) {
        return switch (value) {
            case 1 -> MessageType.ClientHello;
            case 2 -> MessageType.ServerHello;
            case 3 -> MessageType.Listen;
            case 4 -> MessageType.ListenAck;
            case 5 -> MessageType.DataMessage;
            default -> throw new InvalidMessageType(value);
        };
    }

    public int getRaw() {
        return switch (this) {
            case Listen -> 3;
            case ServerHello -> 2;
            case ListenAck -> 4;
            case DataMessage -> 5;
            default -> 1;
        };
    }
}
