package dev.mgbarbosa.jtcproxy.server;

import dev.mgbarbosa.jtcproxy.exceptions.DisposedPortException;

/** 
  * Represents a reserved port from {@link PortProvider}
  **/
public class ServerPortImpl implements ServerPort, Port, AutoCloseable {
    private final PortProvider portProvider;
    private final Integer port;
    private boolean disposed;

    public ServerPortImpl(final PortProvider provider, final Integer port) {
        this.portProvider = provider;
        this.port = port;
        this.disposed = false;
    }

    /**
      * Gets reserver port.
      * @throws IllegalAccessException if port is already disposed.
      * */
    public Integer getPort() throws DisposedPortException {
        return getPort(true);
    }

    /**
      * {@inheritDoc}
      **/
    @Override
    public Integer getPort(final boolean throwIfDisposed) {
        if (this.disposed && throwIfDisposed) {
            throw new DisposedPortException(this);
        }

        return this.port;
    }

    public boolean isDisposed() {
        return this.disposed;
    }

    @Override
    public void close() throws Exception {
        portProvider.dispose(this);
        this.disposed = true;
    }
}
