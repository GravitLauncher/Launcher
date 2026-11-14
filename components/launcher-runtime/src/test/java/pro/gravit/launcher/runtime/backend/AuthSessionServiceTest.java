package pro.gravit.launcher.runtime.backend;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthSessionServiceTest {
    @Test
    void coalescesConcurrentRefreshes() {
        var sessions = new AuthSessionService();
        long generation = sessions.current();
        var requests = new AtomicInteger();
        var refresh = new CompletableFuture<String>();

        var first = sessions.singleFlight(generation, () -> {
            requests.incrementAndGet();
            return refresh;
        });
        var second = sessions.singleFlight(generation, () -> {
            requests.incrementAndGet();
            return CompletableFuture.completedFuture("unexpected");
        });
        refresh.complete("token");

        assertEquals("token", first.join());
        assertEquals("token", second.join());
        assertEquals(1, requests.get());
    }

    @Test
    void rejectsRefreshCompletionAfterIdentityChanges() {
        var sessions = new AuthSessionService();
        long generation = sessions.current();
        var refresh = new CompletableFuture<String>();
        var result = sessions.singleFlight(generation, () -> refresh);

        sessions.advance();
        refresh.complete("stale-token");

        assertThrows(CompletionException.class, result::join);
    }

    @Test
    void cancellingOneWaiterDoesNotCancelTheSharedRefresh() {
        var sessions = new AuthSessionService();
        long generation = sessions.current();
        var refresh = new CompletableFuture<String>();
        var first = sessions.singleFlight(generation, () -> refresh);
        var second = sessions.singleFlight(generation, () -> CompletableFuture.completedFuture("unexpected"));

        first.cancel(true);
        refresh.complete("token");

        assertEquals("token", second.join());
    }
}
