package dev.mgbarbosa.jtcproxy.server;

/**
 * Stores available port to be reserved.
 */
public interface PortProvider {
    /**
     * Reserves new random port from available pool of ports.
     * 
     * @throws IllegalStateException when no ports are avaiable in the pool.
     **/
    ServerPort get();

    /**
     * Disposes of given port, removing it from available port pool.
     * Note that after disposing a port, calling {@link ServerPort#getPort()} method
     * will throw InvalidAccessException
     * if port is already disposed.
     **/
    void dispose(final Port serverPort);
}
