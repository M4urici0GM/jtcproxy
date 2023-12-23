package dev.mgbarbosa.jtcproxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;

public class CancellableInputStream extends InputStream {

    private static final int STEP = 200000;  // in nanoseconds from 0 to 999999 (inclusive)
    private final InputStream stream;
    private final CancellationToken cancellationToken;

    private boolean shouldContinue = true;

    public CancellableInputStream(InputStream stream, final CancellationToken cancellationToken) {
        super();
        this.stream = stream;
        this.cancellationToken = cancellationToken;
    }


    @Override
    public int read() throws IOException {
        int a = this.stream.available();
        if (a >= 1) {
            return this.stream.read();
        }
        while (this.shouldContinue) {
            try {
                Thread.sleep(0, STEP);
            } catch (InterruptedException e) {
                throw new IOException("Waiting operation stopped.", e);
            }

            if (this.stream.available() == 0) {
                if (cancellationToken.isCancellationRequested()) {
                    throw new InterruptedIOException("task was cancelled");
                }

                continue;
            }


            return this.stream.read();
        }

        throw new IOException("stream is closed");
    }

    @Override
    public void close() throws IOException {
        this.shouldContinue = false;
        super.close();
    }
}
