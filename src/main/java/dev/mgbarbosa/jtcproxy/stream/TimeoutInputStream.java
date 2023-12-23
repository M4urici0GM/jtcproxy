package dev.mgbarbosa.jtcproxy.stream;

import java.io.IOException;
import java.io.InputStream;

public class TimeoutInputStream extends InputStream {

    private static final int STEP = 200000;  // in nanoseconds from 0 to 999999 (inclusive)

    private final long timeout;
    private final InputStream stream;

    private volatile boolean looped = true;

    public TimeoutInputStream(InputStream stream, int timeout) {
        super();
        this.timeout = 1000000L * timeout;
        this.stream = stream;
    }

    @Override
    public int read() throws IOException {
        int a = this.stream.available();
        if (a >= 1) {
            return this.stream.read();
        }
        long t1 = System.nanoTime();
        while (this.looped) {
            try {
                Thread.sleep(0, STEP);
            } catch (InterruptedException e) {
                throw new IOException("Waiting operation stopped.", e);
            }
            int b = this.stream.available();
            if (b == 0) {
                long t2 = System.nanoTime();
                if (t2 - t1 > timeout) {
                    throw new IOException("Read operation timeout.");
                }
            } else {
                return this.stream.read();
            }
        }
        throw new IOException("Stream is closed.");
    }

    @Override
    public void close() throws IOException {
        this.looped = false;  // only to speed up read() method breaking
        super.close();
    }
}
