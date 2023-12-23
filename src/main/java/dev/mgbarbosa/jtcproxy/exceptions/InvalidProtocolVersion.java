package dev.mgbarbosa.jtcproxy.exceptions;

public class InvalidProtocolVersion extends RuntimeException {
    private final int value;

    public InvalidProtocolVersion(final int value) {
        super();
        this.value = value;
    }
}
