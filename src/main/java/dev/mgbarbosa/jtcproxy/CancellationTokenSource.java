package dev.mgbarbosa.jtcproxy;

import java.util.concurrent.atomic.AtomicBoolean;

public class CancellationTokenSource {
    private final AtomicBoolean value;
    private final CancellationToken cancellationToken;

    public CancellationTokenSource() {
        this.value = new AtomicBoolean(false);
        this.cancellationToken = CancellationToken.createFrom(value);
    }

    public CancellationToken getToken() {
        return this.cancellationToken;
    }

    public void cancel() {
        this.value.set(true);
    }
}
