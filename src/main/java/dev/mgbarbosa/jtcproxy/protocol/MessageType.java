package dev.mgbarbosa.jtcproxy.protocol;

import dev.mgbarbosa.jtcproxy.exceptions.InvalidMessageType;

public enum MessageType {
    ClientHello,
    ServerHello;

    public static MessageType of(int value) {
        return switch (value) {
            case 1 -> MessageType.ClientHello;
            case 2 -> MessageType.ServerHello;
            default -> throw new InvalidMessageType(value);
        };
    }

    public int getRaw() {
        return switch (this) {
            case ServerHello -> 2;
            default -> 1;
        };
    }
}
