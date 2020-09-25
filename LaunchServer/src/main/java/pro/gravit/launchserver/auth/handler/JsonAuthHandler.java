package pro.gravit.launchserver.auth.handler;

import pro.gravit.launcher.HTTPRequest;
import pro.gravit.launcher.Launcher;

import java.io.IOException;
import java.net.URL;
import java.util.UUID;

public class JsonAuthHandler extends CachedAuthHandler {
    public URL getUrl;
    public URL updateAuthUrl;
    public URL updateServerIdUrl;
    public String apiKey;

    @Override
    protected Entry fetchEntry(String username) throws IOException {
        return Launcher.gsonManager.gson.fromJson(HTTPRequest.jsonRequest(Launcher.gsonManager.gson.toJsonTree(new EntryRequestByUsername(username, apiKey)), getUrl), Entry.class);
    }

    @Override
    protected Entry fetchEntry(UUID uuid) throws IOException {
        return Launcher.gsonManager.gson.fromJson(HTTPRequest.jsonRequest(Launcher.gsonManager.gson.toJsonTree(new EntryRequestByUUID(uuid, apiKey)), getUrl), Entry.class);
    }

    @Override
    protected boolean updateAuth(UUID uuid, String username, String accessToken) throws IOException {
        return Launcher.gsonManager.gson.fromJson(HTTPRequest.jsonRequest(Launcher.gsonManager.gson.toJsonTree(new UpdateAuthRequest(uuid, username, accessToken, apiKey)), updateAuthUrl), SuccessResponse.class).success;
    }

    @Override
    protected boolean updateServerID(UUID uuid, String serverID) throws IOException {
        return Launcher.gsonManager.gson.fromJson(HTTPRequest.jsonRequest(Launcher.gsonManager.gson.toJsonTree(new UpdateServerIDRequest(uuid, serverID, apiKey)), updateServerIdUrl), SuccessResponse.class).success;
    }

    @Override
    public void close() {

    }

    public static class EntryRequestByUsername {
        public final String username;
        public final String apiKey;

        public EntryRequestByUsername(String username, String apiKey) {
            this.username = username;
            this.apiKey = apiKey;
        }
    }

    public static class EntryRequestByUUID {
        public final UUID uuid;
        public final String apiKey;

        public EntryRequestByUUID(UUID uuid, String apiKey) {
            this.uuid = uuid;
            this.apiKey = apiKey;
        }
    }

    public static class UpdateAuthRequest {
        public final UUID uuid;
        public final String username;
        public final String accessToken;
        public final String apiKey;

        public UpdateAuthRequest(UUID uuid, String username, String accessToken, String apiKey) {
            this.uuid = uuid;
            this.username = username;
            this.accessToken = accessToken;
            this.apiKey = apiKey;
        }
    }

    public static class UpdateServerIDRequest {
        public final UUID uuid;
        public final String serverID;
        public final String apiKey;

        public UpdateServerIDRequest(UUID uuid, String serverID, String apiKey) {
            this.uuid = uuid;
            this.serverID = serverID;
            this.apiKey = apiKey;
        }
    }

    public static class SuccessResponse {
        public boolean success;
    }
}
