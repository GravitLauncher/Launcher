package pro.gravit.launcher.runtime.backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launcher.core.api.features.ProfileFeatureAPI;
import pro.gravit.launcher.core.backend.LauncherBackendAPI.ServerPingInfo;
import pro.gravit.launcher.runtime.client.ServerPinger;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

final class ServerPingService {
    private static final Logger logger = LoggerFactory.getLogger(ServerPingService.class);

    private record PingKey(UUID profileId, boolean aggregate) {}
    private final Map<PingKey, CompletableFuture<ServerPingInfo>> pingFutures = new ConcurrentHashMap<>();
    private final ExecutorService pingExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @FunctionalInterface
    interface Ping {
        ServerPinger.Result ping(ClientProfile profile, ClientProfile.ServerProfile server) throws IOException;
    }

    private final Ping ping;

    ServerPingService() {
        this((profile, server) -> server == null ? new ServerPinger(profile).ping()
                : new ServerPinger(server, profile.getVersion()).ping());
    }

    ServerPingService(Ping ping) {
        this.ping = ping;
    }

    CompletableFuture<ServerPingInfo> pingServer(ProfileFeatureAPI.ClientProfile profile) {
        PingKey key = new PingKey(profile.getUUID(), false);
        return share(key, () -> CompletableFuture.supplyAsync(() -> {
            try {
                return ping.ping((ClientProfile) profile, null);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }, pingExecutor));
    }

    CompletableFuture<ServerPingInfo> pingProfileServers(ProfileFeatureAPI.ClientProfile profile) {
        PingKey key = new PingKey(profile.getUUID(), true);
        return share(key, () -> CompletableFuture.completedFuture(profile)
                .thenComposeAsync(value -> aggregate((ClientProfile) value), pingExecutor));
    }

    private CompletableFuture<ServerPingInfo> share(
            PingKey key, Supplier<CompletableFuture<ServerPingInfo>> start) {
        var future = pingFutures.compute(key, (ignored, current) ->
                current == null || current.isDone() ? start.get() : current);
        future.whenComplete((value, error) -> pingFutures.remove(key, future));
        return future;
    }

    private CompletableFuture<ServerPingInfo> aggregate(ClientProfile profile) {
        var servers = profile.getServers();
        if (servers == null || servers.isEmpty()) {
            return CompletableFuture.completedFuture(new Totals(0, 0));
        }

        var requests = servers.stream()
                .map(server -> CompletableFuture.supplyAsync(() -> pingServer(profile, server), pingExecutor))
                .toList();
        return CompletableFuture.allOf(requests.toArray(CompletableFuture<?>[]::new))
                .thenApply(ignored -> {
                    int online = 0;
                    int maximum = 0;
                    boolean available = false;
                    for (var request : requests) {
                        var result = request.join();
                        if (result == null) continue;
                        online += result.getOnline();
                        maximum += result.getMaxOnline();
                        available = true;
                    }
                    if (!available) throw new CompletionException(new IOException("All servers are unavailable"));
                    return new Totals(online, maximum);
                });
    }

    private ServerPinger.Result pingServer(ClientProfile profile, ClientProfile.ServerProfile server) {
        try {
            return ping.ping(profile, server);
        } catch (IOException e) {
            logger.warn("Failed to ping server {}: {}", server.name, e.getMessage());
            return null;
        }
    }

    private record Totals(int getOnline, int getMaxOnline) implements ServerPingInfo {
        @Override
        public List<String> getPlayerNames() {
            return List.of();
        }
    }

    void clear() {
        for (var future : pingFutures.values()) future.cancel(true);
        pingFutures.clear();
    }

    void close() {
        clear();
        pingExecutor.shutdownNow();
    }
}
