package ru.gravit.launchserver.auth.handler;

import ru.gravit.launchserver.LaunchServer;
import ru.gravit.utils.HTTPRequest;

import java.io.IOException;
import java.net.URL;
import java.util.UUID;

public class JsonAuthHandler extends CachedAuthHandler {
    public URL getUrl;
    public URL updateAuthUrl;
    public URL updateServerIdUrl;
    public class EntryRequestByUsername
    {
        public String username;

        public EntryRequestByUsername(String username) {
            this.username = username;
        }
    }
    public class EntryRequestByUUID
    {
        public UUID uuid;

        public EntryRequestByUUID(UUID uuid) {
            this.uuid = uuid;
        }
    }
    public class UpdateAuthRequest
    {
        public UUID uuid;
        public String username;
        public String accessToken;

        public UpdateAuthRequest(UUID uuid, String username, String accessToken) {
            this.uuid = uuid;
            this.username = username;
            this.accessToken = accessToken;
        }
    }
    public class UpdateServerIDRequest
    {
        public UUID uuid;
        public String serverID;

        public UpdateServerIDRequest(UUID uuid, String serverID) {
            this.uuid = uuid;
            this.serverID = serverID;
        }
    }
    public class SuccessResponse
    {
        public boolean success;
    }
    @Override
    protected Entry fetchEntry(String username) throws IOException {
        return LaunchServer.gson.fromJson(HTTPRequest.jsonRequest(LaunchServer.gson.toJsonTree(new EntryRequestByUsername(username)), getUrl), Entry.class);
    }

    @Override
    protected Entry fetchEntry(UUID uuid) throws IOException {
        return LaunchServer.gson.fromJson(HTTPRequest.jsonRequest(LaunchServer.gson.toJsonTree(new EntryRequestByUUID(uuid)), getUrl), Entry.class);
    }

    @Override
    protected boolean updateAuth(UUID uuid, String username, String accessToken) throws IOException {
        return LaunchServer.gson.fromJson(HTTPRequest.jsonRequest(LaunchServer.gson.toJsonTree(new UpdateAuthRequest(uuid, username, accessToken)), updateAuthUrl), SuccessResponse.class).success;
    }

    @Override
    protected boolean updateServerID(UUID uuid, String serverID) throws IOException {
        return LaunchServer.gson.fromJson(HTTPRequest.jsonRequest(LaunchServer.gson.toJsonTree(new UpdateServerIDRequest(uuid, serverID)), updateServerIdUrl), SuccessResponse.class).success;
    }

    @Override
    public void close() throws IOException {

    }
}
