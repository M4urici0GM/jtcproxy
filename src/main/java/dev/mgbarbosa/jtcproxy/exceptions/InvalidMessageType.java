package dev.mgbarbosa.jtcproxy.exceptions;

public class InvalidMessageType extends RuntimeException {
    private final int value;

    public InvalidMessageType(final int value) {
        super();
        this.value = value;
    }
}
