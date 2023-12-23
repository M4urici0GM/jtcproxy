package dev.mgbarbosa.jtcproxy;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import dev.mgbarbosa.jtcproxy.cancellation.CancellationTokenSource;
import sun.misc.Signal;
import sun.misc.SignalHandler;


public class App {
    public static void main(String[] args) throws InterruptedException {
        final var handler = new SignalHandler() {
            public void handle(Signal sig) {
                System.out.println(" howdy, some dude sent an sig: " + sig);
            }
        };

        Signal.handle(new Signal("INT"), handler);
        Signal.handle(new Signal("TERM"), handler);

        final var executor = Executors.newVirtualThreadPerTaskExecutor();
        final var cancellationTokenSource = new CancellationTokenSource();

        executor.submit(() -> {
            final var token = cancellationTokenSource.getToken();
            try  {
                while (!token.isCancellationRequested()) {
                    System.out.println("aaaaaA");
                    Thread.sleep(1000);
                }

                System.out.println("shutdown requested");
            } catch (InterruptedException ex) {
                System.out.println("thread cancel requested.");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });


        executor.submit(() -> {
            try {
                final var token = cancellationTokenSource.getToken();
                while (!token.isCancellationRequested()) {
                    System.out.println("aaaaaA");
                    Thread.sleep(1000);
                }

                System.out.println("shutdown requested");
            } catch (InterruptedException ex) {
                System.out.println("thread cancel requested.");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        Thread.sleep(2000);
        cancellationTokenSource.cancel();
        executor.shutdown();
        final var result = executor.awaitTermination(5000, TimeUnit.MILLISECONDS);

        System.out.println("Result: " + result);
    }
}
