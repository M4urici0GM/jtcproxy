package dev.mgbarbosa.jtcproxy.server;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang3.Range;

/**
  * {@inheritDoc}
  **/
public class DefaultPortProvider implements PortProvider {
    private final Range<Integer> portRange;
    private final Set<Integer> availablePorts;
    private final Random random = new Random();

    public DefaultPortProvider(final int min, final int max) {
        this.portRange = createRange(min, max);
        this.availablePorts = new HashSet<>();

        for (int i = portRange.getMinimum(); i < portRange.getMaximum(); i++) {
            availablePorts.add(i);
        }
    }

    public static Range<Integer> createRange(final Integer min, final Integer max) {
        return Range.of(min, max);
    }

    public static PortProvider getDefault() {
        return new DefaultPortProvider(10_000, 20_000);
    }

    @Override
    public ServerPort get() throws IllegalStateException {
        if (availablePorts.isEmpty()) {
            throw new IllegalStateException("There's no available port for reserving.");
        }

        final var index = random.nextInt(availablePorts.size());
        final var port = availablePorts.stream()
            .skip(index)
            .findFirst()
            .orElseThrow();

        availablePorts.remove(port);
        return new ServerPortImpl(this, port);
    }

    @Override
    public void dispose(final Port serverPort) {
        if (serverPort.isDisposed()) {
            return;
        }

        
        this.availablePorts.add(serverPort.getPort(false));
    }

}
