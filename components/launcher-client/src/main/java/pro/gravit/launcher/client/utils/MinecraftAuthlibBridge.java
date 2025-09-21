package pro.gravit.launcher.client.utils;

import com.gravitlauncher.socketbridge.EmptyResponse;
import com.gravitlauncher.socketbridge.ErrorResponse;
import com.gravitlauncher.socketbridge.MessageHeader;
import com.gravitlauncher.socketbridge.Registry;
import com.gravitlauncher.socketbridge.minecraft.MinecraftRegistries;
import com.gravitlauncher.socketbridge.minecraft.PropertyEntity;
import com.gravitlauncher.socketbridge.minecraft.UserEntity;
import com.gravitlauncher.socketbridge.minecraft.requests.*;
import com.gravitlauncher.socketbridge.server.SocketServer;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.core.api.LauncherAPI;
import pro.gravit.launcher.core.api.LauncherAPIHolder;
import pro.gravit.launcher.core.api.features.UserFeatureAPI;
import pro.gravit.launcher.core.api.model.Texture;
import pro.gravit.launcher.core.api.model.User;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;

public class MinecraftAuthlibBridge extends SocketServer {

    public static String SESSION_HARDWARE_ID_PROPERTY = "session_hardware_id";
    public static String SESSION_ID_PROPERTY = "session_id";
    public static String SESSION_BASE_PROPERTY = "session_";

    public MinecraftAuthlibBridge(SocketAddress address) throws IOException {
        super(MinecraftRegistries.makeResponseRegistry(), MinecraftRegistries.makeRequestRegistry(), address);
        registerHandler(registryResponse.get(UserByUsernameRequest.class), this::onUserByUsernameRequest);
        registerHandler(registryResponse.get(UserByUuidRequest.class), this::onUserByUuidRequest);
        registerHandler(registryResponse.get(JoinServerByUsernameRequest.class), this::onJoinServerByUsernameRequest);
        registerHandler(registryResponse.get(JoinServerByUuidRequest.class), this::onJoinServerByUuidRequest);
        registerHandler(registryResponse.get(CheckServerRequest.class), this::onCheckServerRequest);
    }

    public void onUserByUsernameRequest(MessageHeader header, Object o, ServerClient client) {
        UserByUsernameRequest userByUsernameRequest = (UserByUsernameRequest) o;
        LauncherAPIHolder.user().getUserByUsername(userByUsernameRequest.username).thenApply(this::convertToUserEntity).thenAccept(e -> {
            client.sendResponse(header, e);
        }).exceptionally(ex -> {
            if(ex instanceof CompletionException) {
                ex = ex.getCause();
            }
            client.sendResponse(header, new ErrorResponse(ex.getMessage()));
            return null;
        });
    }

    public void onUserByUuidRequest(MessageHeader header, Object o, ServerClient client) {
        UserByUuidRequest userByUuidRequest = (UserByUuidRequest) o;
        LauncherAPIHolder.user().getUserByUUID(userByUuidRequest.uuid).thenApply(this::convertToUserEntity).thenAccept(e -> {
            client.sendResponse(header, e);
        }).exceptionally(ex -> {
            if(ex instanceof CompletionException) {
                ex = ex.getCause();
            }
            client.sendResponse(header, new ErrorResponse(ex.getMessage()));
            return null;
        });
    }

    public void onJoinServerByUsernameRequest(MessageHeader header, Object o, ServerClient client) {
        JoinServerByUsernameRequest joinServerByUsernameRequest = (JoinServerByUsernameRequest) o;
        LauncherAPIHolder.user().joinServer(joinServerByUsernameRequest.username, joinServerByUsernameRequest.accessToken, joinServerByUsernameRequest.serverId)
                .thenAccept(e -> {
            client.sendResponse(header, new EmptyResponse());
        }).exceptionally(ex -> {
            if(ex instanceof CompletionException) {
                ex = ex.getCause();
            }
            client.sendResponse(header, new ErrorResponse(ex.getMessage()));
            return null;
        });
    }

    public void onJoinServerByUuidRequest(MessageHeader header, Object o, ServerClient client) {
        JoinServerByUuidRequest joinServerByUuidRequest = (JoinServerByUuidRequest) o;
        LauncherAPIHolder.user().joinServer(joinServerByUuidRequest.uuid, joinServerByUuidRequest.accessToken, joinServerByUuidRequest.serverId)
                .thenAccept(e -> {
                    client.sendResponse(header, new EmptyResponse());
                }).exceptionally(ex -> {
                    if(ex instanceof CompletionException) {
                        ex = ex.getCause();
                    }
                    client.sendResponse(header, new ErrorResponse(ex.getMessage()));
                    return null;
                });
    }

    public void onCheckServerRequest(MessageHeader header, Object o, ServerClient client) {
        CheckServerRequest request = (CheckServerRequest) o;
        LauncherAPIHolder.user().checkServer(request.username, request.serverId, true)
                .thenApply(this::convertToUserEntity)
                .thenAccept(e -> {
                    client.sendResponse(header, e);
                }).exceptionally(ex -> {
                    if(ex instanceof CompletionException) {
                        ex = ex.getCause();
                    }
                    client.sendResponse(header, new ErrorResponse(ex.getMessage()));
                    return null;
                });
    }

    public UserEntity convertToUserEntity(User user) {
        UserEntity entity = new UserEntity();
        entity.id = user.getUUID();
        entity.name = user.getUsername();
        entity.properties = new HashMap<>();
        if(user.getProperties() != null) {
            for(var e : user.getProperties().entrySet()) {
                pushProperty(entity.properties, makeProperty(e.getKey(), e.getValue()));
            }
        }
        pushProperty(entity.properties, makeTexturesProperty(user));
        return entity;
    }

    public UserEntity convertToUserEntity(UserFeatureAPI.CheckServerResponse checkServerResponse) {
        UserEntity entity = convertToUserEntity(checkServerResponse.user());
        pushProperty(entity.properties, makeProperty(SESSION_ID_PROPERTY, checkServerResponse.sessionId()));
        pushProperty(entity.properties, makeProperty(SESSION_HARDWARE_ID_PROPERTY, checkServerResponse.hardwareId()));
        if(checkServerResponse.sessionProperties() != null) {
            for(var e : checkServerResponse.sessionProperties().entrySet()) {
                pushProperty(entity.properties, makeProperty(SESSION_BASE_PROPERTY.concat(e.getKey()), e.getValue()));
            }
        }
        return entity;
    }

    public static PropertyEntity makeProperty(String name, String value) {
        PropertyEntity propertyEntity = new PropertyEntity();
        propertyEntity.name = name;
        propertyEntity.value = value;
        propertyEntity.signature = "";
        return propertyEntity;
    }

    public static void pushProperty(Map<String, PropertyEntity> properties, PropertyEntity property) {
        if (property == null) {
            return;
        }
        properties.put(property.name, property);
    }

    public static PropertyEntity makeTexturesProperty(User playerProfile) {
        Map<String, Texture> textures = playerProfile.getAssets();
        Map<String, GenericTexture> genericTextures = new HashMap<>();
        for(var e : textures.entrySet()) {
            genericTextures.put(e.getKey(), toGenericTexture(e.getValue()));
        }
        MinecraftTexturesProperty property = new MinecraftTexturesProperty();
        property.timestamp = System.currentTimeMillis();
        property.profileId = playerProfile.getUUID().toString().replace("-", "");
        property.profileName = playerProfile.getUsername();
        property.isPublic = true;
        property.textures = genericTextures;
        String encoded = Launcher.gsonManager.gson.toJson(property);
        String base64 = Base64.getEncoder().encodeToString(encoded.getBytes(StandardCharsets.UTF_8));
        return makeProperty("textures", base64);
    }

    private static GenericTexture toGenericTexture(Texture texture) {
        return new GenericTexture(texture.getUrl(), texture.getHash(), texture.getMetadata());
    }

    public static class MinecraftTexturesProperty {
        public long timestamp;
        public String profileId;
        public String profileName;
        public boolean isPublic;
        public Map<String, GenericTexture> textures;

        public MinecraftTexturesProperty() {
        }

        public MinecraftTexturesProperty(long timestamp, String profileId, String profileName, boolean isPublic, Map<String, GenericTexture> textures) {
            this.timestamp = timestamp;
            this.profileId = profileId;
            this.profileName = profileName;
            this.isPublic = isPublic;
            this.textures = textures;
        }
    }

    public static class GenericTexture {
        private final String url;
        private final String hash;
        private final Map<String, String> metadata;

        public GenericTexture(String url, String hash, Map<String, String> metadata) {
            this.url = url;
            this.hash = hash;
            this.metadata = metadata;
        }

        public String getUrl() {
            return url;
        }

        public String getHash() {
            return hash;
        }

        public Map<String, String> getMetadata() {
            return metadata;
        }
    }
}
