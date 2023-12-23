package dev.mgbarbosa.jtcproxy.protocol;

import dev.mgbarbosa.jtcproxy.exceptions.InvalidProtocolVersion;

public enum ProtocolVersion {
    Version1;

    public static ProtocolVersion of(int value) {
        return switch (value) {
            case 1 -> ProtocolVersion.Version1;
            default -> throw new InvalidProtocolVersion(value);
        };
    }

    public int getRaw() {
        return switch (this) {
            default -> 1;
        };
    }
}
