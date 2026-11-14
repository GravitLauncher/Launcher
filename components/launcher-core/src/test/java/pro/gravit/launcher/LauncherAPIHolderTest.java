package pro.gravit.launcher;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import pro.gravit.launcher.core.api.LauncherAPI;
import pro.gravit.launcher.core.api.LauncherAPIHolder;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class LauncherAPIHolderTest {
    @AfterEach
    void reset() { LauncherAPIHolder.reset(); }

    @Test
    void resetClosesSharedTransportsOnceAndDiscardsCachedAccounts() {
        AtomicInteger closes = new AtomicInteger();
        AutoCloseable transport = closes::incrementAndGet;
        LauncherAPIHolder.registerResource(transport);
        LauncherAPIHolder.registerResource(transport);
        LauncherAPIHolder.setCreateApiFactory(id -> new LauncherAPI(Map.of()));
        LauncherAPI previous = LauncherAPIHolder.get("account");

        LauncherAPIHolder.reset();
        LauncherAPIHolder.reset();
        assertEquals(1, closes.get());
        assertNull(LauncherAPIHolder.get());
        LauncherAPIHolder.setCreateApiFactory(id -> new LauncherAPI(Map.of()));
        assertNotSame(previous, LauncherAPIHolder.get("account"));
    }
}
