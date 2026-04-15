package wings.v.rustore.parser;

import java.time.Duration;

final class RequestThrottler {
    private final long delayMillis;
    private long nextAllowedAtMillis;

    RequestThrottler(Duration delay) {
        this.delayMillis = Math.max(0L, delay.toMillis());
    }

    synchronized void awaitTurn() throws InterruptedException {
        if (delayMillis <= 0L) {
            return;
        }

        long now = System.currentTimeMillis();
        if (nextAllowedAtMillis > now) {
            Thread.sleep(nextAllowedAtMillis - now);
            now = System.currentTimeMillis();
        }
        nextAllowedAtMillis = now + delayMillis;
    }
}
