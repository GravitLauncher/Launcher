package pro.gravit.launcher.runtime.backend;

import pro.gravit.launcher.core.backend.exceptions.LauncherBackendException;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/** Tracks identity changes and coalesces refreshes belonging to the active session. */
final class AuthSessionService {
    private final Object lock = new Object();
    private long generation;
    private CompletableFuture<?> refreshFuture;

    Object lock() { return lock; }

    long current() {
        synchronized (lock) { return generation; }
    }

    long advance() {
        synchronized (lock) {
            generation++;
            refreshFuture = null;
            return generation;
        }
    }

    boolean isCurrent(long expected) {
        synchronized (lock) { return expected == generation; }
    }

    @SuppressWarnings("unchecked")
    <T> CompletableFuture<T> singleFlight(long expected, Supplier<CompletableFuture<T>> supplier) {
        synchronized (lock) {
            if (expected != generation) return CompletableFuture.failedFuture(new LauncherBackendException("Authentication session changed"));
            if (refreshFuture != null && !refreshFuture.isDone()) {
                return ((CompletableFuture<T>) refreshFuture).thenApply(value -> value);
            }
            CompletableFuture<T> shared = supplier.get().thenApply(value -> {
                if (!isCurrent(expected)) throw new LauncherBackendException("Authentication session changed");
                return value;
            });
            refreshFuture = shared;
            shared.whenComplete((value, error) -> {
                synchronized (lock) {
                    if (refreshFuture == shared) refreshFuture = null;
                }
            });
            return shared.thenApply(value -> value);
        }
    }
}
