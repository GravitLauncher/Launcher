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

    @Override
    protected Entry fetchEntry(String username) throws IOException {
        return Launcher.gsonManager.configGson.fromJson(HTTPRequest.jsonRequest(Launcher.gsonManager.configGson.toJsonTree(new EntryRequestByUsername(username)), getUrl), Entry.class);
    }

    @Override
    protected Entry fetchEntry(UUID uuid) throws IOException {
        return Launcher.gsonManager.configGson.fromJson(HTTPRequest.jsonRequest(Launcher.gsonManager.configGson.toJsonTree(new EntryRequestByUUID(uuid)), getUrl), Entry.class);
    }

    @Override
    protected boolean updateAuth(UUID uuid, String username, String accessToken) throws IOException {
        return Launcher.gsonManager.configGson.fromJson(HTTPRequest.jsonRequest(Launcher.gsonManager.configGson.toJsonTree(new UpdateAuthRequest(uuid, username, accessToken)), updateAuthUrl), SuccessResponse.class).success;
    }

    @Override
    protected boolean updateServerID(UUID uuid, String serverID) throws IOException {
        return Launcher.gsonManager.configGson.fromJson(HTTPRequest.jsonRequest(Launcher.gsonManager.configGson.toJsonTree(new UpdateServerIDRequest(uuid, serverID)), updateServerIdUrl), SuccessResponse.class).success;
    }

    @Override
    public void close() {

    }

    public static class EntryRequestByUsername {
        public final String username;

        public EntryRequestByUsername(String username) {
            this.username = username;
        }
    }

    public static class EntryRequestByUUID {
        public final UUID uuid;

        public EntryRequestByUUID(UUID uuid) {
            this.uuid = uuid;
        }
    }

    public static class UpdateAuthRequest {
        public final UUID uuid;
        public final String username;
        public final String accessToken;

        public UpdateAuthRequest(UUID uuid, String username, String accessToken) {
            this.uuid = uuid;
            this.username = username;
            this.accessToken = accessToken;
        }
    }

    public static class UpdateServerIDRequest {
        public final UUID uuid;
        public final String serverID;

        public UpdateServerIDRequest(UUID uuid, String serverID) {
            this.uuid = uuid;
            this.serverID = serverID;
        }
    }

    public static class SuccessResponse {
        public boolean success;
    }
}
