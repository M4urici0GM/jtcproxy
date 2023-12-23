package dev.mgbarbosa.jtcproxy.exceptions;

import static java.lang.String.format;

import dev.mgbarbosa.jtcproxy.server.Port;

public class DisposedPortException extends RuntimeException {
    public DisposedPortException(final Port string) {
        super(format("Port %s is already disposed.", string.getPort(false)));
    }
}
