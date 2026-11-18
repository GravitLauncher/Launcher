package pro.gravit.launcher.runtime.backend;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launcher.runtime.client.ServerPinger;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ServerPingServiceTest {
    @Test
    void sharesRequestsAndAllowsRetryAndSessionCleanup() throws Exception {
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var service = new ServerPingService((profile, server) -> {
            entered.countDown();
            try {
                if (!release.await(5, TimeUnit.SECONDS)) throw new IOException("Ping was not released");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException(e);
            }
            return new ServerPinger.Result(2, 10, "");
        });
        try {
            var profile = profile("[]");
            var first = service.pingServer(profile);
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            assertSame(first, service.pingServer(profile));

            service.clear();
            assertTrue(first.isCancelled());
            var retry = service.pingServer(profile);
            assertNotSame(first, retry);
            release.countDown();
            assertEquals(2, retry.get(5, TimeUnit.SECONDS).getOnline());
            assertNotSame(retry, service.pingServer(profile));
        } finally {
            release.countDown();
            service.close();
        }
    }

    @Test
    void aggregatesAvailableServersAndReportsTotalFailure() throws Exception {
        var service = new ServerPingService((profile, server) -> {
            if ("unavailable".equals(server.name)) throw new IOException("Unavailable");
            return new ServerPinger.Result(2, 10, "");
        });
        try {
            var partial = service.pingProfileServers(profile("""
                    [{"name":"available"},{"name":"unavailable"}]
                    """)).get(5, TimeUnit.SECONDS);
            assertEquals(2, partial.getOnline());
            assertEquals(10, partial.getMaxOnline());
            assertEquals(0, service.pingProfileServers(profile("[]")).get(5, TimeUnit.SECONDS).getOnline());
            var failure = assertThrows(ExecutionException.class, () ->
                    service.pingProfileServers(profile("[{\"name\":\"unavailable\"}]")).get(5, TimeUnit.SECONDS));
            assertInstanceOf(IOException.class, failure.getCause());
        } finally {
            service.close();
        }
    }

    private static ClientProfile profile(String servers) {
        return new Gson().fromJson("""
                {"uuid":"d66de957-ccca-42e1-9011-b1f422fe2c54","servers":%s}
                """.formatted(servers), ClientProfile.class);
    }
}
