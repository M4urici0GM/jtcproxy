package dev.mgbarbosa.jtcproxy.server;

public interface  Port {
    /**
      * Returns reserved port.
      * {@throws DisposedPortException} if true is provided and port is already dispoed.
      **/
    Integer getPort(boolean throwIfDisposed);
    boolean isDisposed();
}
