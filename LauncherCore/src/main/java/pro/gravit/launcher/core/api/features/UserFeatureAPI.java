package pro.gravit.launcher.core.api.features;

import pro.gravit.launcher.core.api.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public interface UserFeatureAPI extends FeatureAPI {
    CompletableFuture<User> getUserByUsername(String username);
    CompletableFuture<User> getUserByUUID(UUID uuid);
    CompletableFuture<Void> joinServer(String username, String accessToken, String serverID);
    CompletableFuture<Void> joinServer(UUID uuid, String accessToken, String serverID);
    CompletableFuture<CheckServerResponse> checkServer(String username, String serverID, boolean extended);
    default CompletableFuture<List<User>> getUsersByUsernames(List<String> usernames) {
        List<CompletableFuture<User>> list = new ArrayList<>();
        for(var username : usernames) {
            list.add(getUserByUsername(username));
        }
        return CompletableFuture.allOf(list.toArray(CompletableFuture[]::new)).thenApply(x -> {
            List<User> r = new ArrayList<>();
            for(var e : list) {
                try {
                    r.add(e.get());
                } catch (InterruptedException | ExecutionException ex) {
                    r.add(null);
                }
            }
            return r;
        });
    }

    record CheckServerResponse(User user, String hardwareId, String sessionId, Map<String, String> sessionProperties) {}
}
