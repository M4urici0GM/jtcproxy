package dev.mgbarbosa.jtcproxy;

import java.util.concurrent.atomic.AtomicBoolean;

public record CancellationToken(AtomicBoolean internalValue) {
    public static CancellationToken createFrom(AtomicBoolean internalValue) {
        return new CancellationToken(internalValue);
    }

    public boolean isCancellationRequested() {
        return internalValue.get();
    }

    public void throwIfCancellationRequested() throws InterruptedException {
        if (isCancellationRequested()) {
            throw new InterruptedException("A task interrupt request was received.");
        }
    }
}
