package pro.gravit.launchserver.auth.profiles;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.gravit.launcher.base.Downloader;
import pro.gravit.launcher.base.HttpHelper;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launcher.base.profiles.ClientProfileBuilder;
import pro.gravit.launcher.base.request.RequestFeatureHttpAPIImpl;
import pro.gravit.launcher.core.hasher.HashedDir;
import pro.gravit.launcher.core.hasher.HashedEntry;
import pro.gravit.launcher.core.hasher.HashedFile;
import pro.gravit.launchserver.command.Command;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class RemoteProfilesProvider extends ProfilesProvider {
    private static final Logger log = LoggerFactory.getLogger(RemoteProfilesProvider.class);
    public String baseUrl = "PASTE BASE URL HERE";
    public String accessToken = "PASTE ACCESS TOKEN HERE";
    private final transient HttpClient client = HttpClient.newBuilder().build();
    private final transient Map<String, HttpFileUploadResponse> uploadedBlobs = new HashMap<>();
    @Override
    public UncompletedProfile create(String name, String description, CompletedProfile basic) {
        try {
            UUID uuid = UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HttpHelper.sendAsync(client, HttpRequest.newBuilder()
                            .POST(HttpHelper.jsonBodyPublisher(new CasCreateDirectoryRequest(uuid.toString(), name,
                                    Launcher.gsonManager.gson.toJson(new CasProfileMetadata(uuid, name)))))
                            .uri(URI.create(baseUrl.concat("/cas/directories")))
                            .header("Content-Type", "application/json")
                            .header("Authorization", "Bearer "+accessToken)
                            .build(), new RequestFeatureHttpAPIImpl.HttpErrorHandler<>(CasDirectoryResponse.class))
                    .thenApply(HttpHelper.HttpOptional::getOrThrow).thenCompose(result -> {
                        if(basic == null) {
                            UUID profileUuid = Launcher.gsonManager.gson.fromJson(result.metadata(), CasProfileMetadata.class).uuid();
                            ClientProfile newClientProfile = new ClientProfileBuilder()
                                .setTitle(name)
                                .setInfo(description)
                                .setDir(name)
                                .setUuid(profileUuid)
                                .createClientProfile();
                            return pushUpdateAsync(new HttpUncompletedProfile(profileUuid, newClientProfile), newClientProfile, null, null)
                                    .thenApply(e -> (UncompletedProfile) new HttpUncompletedProfile(profileUuid, newClientProfile));
                        }
                        CasProfileMetadata metadata = Launcher.gsonManager.gson.fromJson(result.metadata(), CasProfileMetadata.class);
                        return CompletableFuture.completedFuture((UncompletedProfile) new HttpUncompletedProfile(metadata.uuid(), basic.getProfile()));
                    }).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(UncompletedProfile profile) {
        throw new UnsupportedOperationException("The new CAS API does not expose directory deletion");
    }

    @Override
    public Set<UncompletedProfile> getProfiles(Client socketClient) {
        try {
            return HttpHelper.sendAsync(client, HttpRequest.newBuilder()
                            .GET()
                            .uri(URI.create(baseUrl.concat("/cas/directories/list")))
                            .header("Authorization", "Bearer "+accessToken)
                            .build(), new RequestFeatureHttpAPIImpl.HttpErrorHandler<>(CasDirectoryListResponse.class))
                    .thenApply(e -> e.getOrThrow().profiles().stream()
                            .map(es -> {
                                CasProfileMetadata metadata = Launcher.gsonManager.gson.fromJson(es.metadata(), CasProfileMetadata.class);
                                return new HttpUncompletedProfile(metadata.uuid(), new ClientProfileBuilder()
                                        .setTitle(metadata.name()).setInfo("").setDir(es.key()).setUuid(metadata.uuid()).createClientProfile());
                            })
                            .map(x -> (UncompletedProfile) x)
                            .collect(Collectors.toSet()))
                    .get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompletedProfile pushUpdate(UncompletedProfile profile, String tag, ClientProfile clientProfile, List<ProfileAction> assetActions, List<ProfileAction> clientActions, List<UpdateFlag> flags) throws IOException {
        var prev = get(profile.getUuid(), tag);
        if(prev != null && prev.getProfile() != null && (prev.getProfile().getUUID() == null || !prev.getProfile().getUUID().equals(prev.getUuid()))) {
            clientProfile = new ClientProfileBuilder(clientProfile)
                    .setUuid(prev.getUuid())
                    .createClientProfile();
        }
        HashedDir clientDir = prev == null ? null : prev.getClientDir();
        HashedDir assetDir = prev == null ? null : prev.getAssetDir();
        if(flags.contains(UpdateFlag.USE_DEFAULT_ASSETS)) {
            assetDir = getUnconnectedDirectory("assets");
        }
        if(assetDir == null) {
            assetDir = new HashedDir();
        }
        if(clientDir == null) {
            clientDir = new HashedDir();
        }
        if(assetActions != null) {
            execute(assetDir, assetActions);
        }
        if(clientActions != null) {
            execute(clientDir, clientActions);
        }

        try {
            return pushUpdateAsync(profile, clientProfile, clientDir, assetDir).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private CompletableFuture<HttpProfile> pushUpdateAsync(UncompletedProfile profile, ClientProfile clientProfile, HashedDir clientDir, HashedDir assetDir) {
        JsonArray files = new JsonArray();
        appendManifest(files, "client", clientDir);
        appendManifest(files, "assets", assetDir);
        JsonObject manifestObject = new JsonObject();
        manifestObject.add("files", files);
        return HttpHelper.sendAsync(client, HttpRequest.newBuilder()
                        .POST(HttpHelper.jsonBodyPublisher(new CasPublishVersionRequest(profile.getUuid().toString(), "main", Launcher.gsonManager.gson.toJson(manifestObject),
                                Launcher.gsonManager.gson.toJson(new HttpProfileMetadata(clientProfile, clientDir, assetDir)), null, null, null)))
                        .uri(URI.create(baseUrl.concat("/cas/versions")))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + accessToken)
                        .build(), new RequestFeatureHttpAPIImpl.HttpErrorHandler<>(CasVersionResponse.class))
                .thenApply(HttpHelper.HttpOptional::getOrThrow)
                .thenApply(version -> new HttpProfile(clientProfile, clientDir == null ? new HashedDir() : clientDir, assetDir == null ? new HashedDir() : assetDir));
    }

    private void appendManifest(JsonArray files, String root, HashedDir dir) {
        if (dir == null || dir.isEmpty()) {
            return;
        }
        try {
        dir.walk("/", (path, name, entry) -> {
            if (entry instanceof HashedFile file) {
                HttpFileUploadResponse blob = uploadedBlobs.get(file.url);
                if (blob == null) {
                    String url = file.url;
                    String hash = url == null ? null : url.substring(url.lastIndexOf('/') + 1);
                    if (hash != null && hash.matches("[0-9a-fA-F]{64}")) {
                        String marker = "/blobs/";
                        int markerIndex = url.indexOf(marker);
                        String storageKey = markerIndex >= 0 ? url.substring(markerIndex + 1) : url;
                        blob = new HttpFileUploadResponse("sha256:" + hash.toLowerCase(Locale.ROOT), "SHA-256", file.size(), storageKey, url);
                    } else {
                        throw new IllegalStateException("Missing CAS blob registration for " + file.url);
                    }
                }
                JsonObject item = new JsonObject();
                item.addProperty("path", root + "/" + path);
                item.addProperty("hash", blob.hash());
                item.addProperty("size", blob.sizeBytes());
                item.addProperty("storageKey", blob.storageKey());
                if (blob.url() != null) {
                    item.addProperty("url", blob.url());
                }
                files.add(item);
            }
            return HashedDir.WalkAction.CONTINUE;
        });
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build CAS manifest", e);
        }
    }

    @Override
    public void download(CompletedProfile profile, Map<String, Path> files, boolean assets) throws IOException {
        HttpProfile httpProfile = (HttpProfile) profile;
        HashedDir dir;
        if(assets) {
            dir = httpProfile.assets;
        } else {
            dir = httpProfile.client;
        }
        List<Downloader.SizedFile> sizedFiles = new ArrayList<>();
        for(var e : files.entrySet()) {
            var key = e.getKey();
            var path = e.getValue();
            if(!key.isEmpty() && !key.equals(".")) {
                var ref = dir.tryFindRecursive(key);
                if(!ref.isFound()) {
                    throw new FileNotFoundException(key);
                }
                if(ref.entry instanceof HashedFile file) {
                    IOHelper.createParentDirs(path);
                    sizedFiles.add(new Downloader.SizedFile(file.url, path.toString(), file.size()));
                } else if(ref.entry instanceof HashedDir hdir) {
                    hdir.walk("/", (rpath, name, entry) -> {
                        if(entry instanceof HashedFile file) {
                            Path target = path.resolve(rpath);
                            try {
                                IOHelper.createParentDirs(target);
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                            sizedFiles.add(new Downloader.SizedFile(file.url, target.toString(), file.size()));
                        }
                        return HashedDir.WalkAction.CONTINUE;
                    });
                }
            } else {
                dir.walk("/", (rpath, name, entry) -> {
                    if(entry instanceof HashedFile file) {
                        Path target = path.resolve(rpath);
                        try {
                            IOHelper.createParentDirs(target);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                        sizedFiles.add(new Downloader.SizedFile(file.url, target.toString(), file.size()));
                    }
                    return HashedDir.WalkAction.CONTINUE;
                });
            }
        }
        log.info("Download {} files", sizedFiles.size());
        try {
            Command.downloadWithProgressBar("files", sizedFiles, "https://example.com", Path.of(""));
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public HashedDir getUnconnectedDirectory(String name) {
        try {
            return HttpHelper.sendAsync(client, HttpRequest.newBuilder()
                            .GET()
                            .uri(URI.create(baseUrl.concat("/cas/versions/latest?directoryKey="+name+"&branchName=main")))
                            .header("Authorization", "Bearer "+accessToken)
                            .build(), new RequestFeatureHttpAPIImpl.HttpErrorHandler<>(HashedDir.class))
                    .thenApply(HttpHelper.HttpOptional::getOrThrow)
                    .get();
        } catch (InterruptedException | ExecutionException e) {
            return null;
        }
    }

    @Override
    public CompletedProfile get(UUID uuid, String tag) {
        try {
            return HttpHelper.sendAsync(client, HttpRequest.newBuilder()
                            .GET()
                            .uri(URI.create(baseUrl.concat("/cas/versions/latest?directoryKey="+uuid+"&branchName=main")))
                            .header("Authorization", "Bearer "+accessToken)
                            .build(), new RequestFeatureHttpAPIImpl.HttpErrorHandler<>(HttpProfile.class))
                    .thenApply(HttpHelper.HttpOptional::getOrThrow)
                    .get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompletedProfile get(String name, String tag) {
        try {
            return HttpHelper.sendAsync(client, HttpRequest.newBuilder()
                            .GET()
                            .uri(URI.create(baseUrl.concat("/cas/versions/latest?directoryKey="+name+"&branchName=main")))
                            .header("Authorization", "Bearer "+accessToken)
                            .build(), new RequestFeatureHttpAPIImpl.HttpErrorHandler<>(HttpProfile.class))
                    .thenApply(HttpHelper.HttpOptional::getOrThrow)
                    .get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private HttpFileUploadResponse uploadFile(byte[] bytes) {
        try {
            String boundary = SecurityHelper.toHex(SecurityHelper.randomBytes(16));
            ByteArrayOutputStream multipart = new ByteArrayOutputStream();
            multipart.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"file\"\r\nContent-Type: application/octet-stream\r\n\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            multipart.write(bytes);
            multipart.write(("\r\n--" + boundary + "--\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HttpHelper.sendAsync(client, HttpRequest.newBuilder()
                            .POST(HttpRequest.BodyPublishers.ofByteArray(multipart.toByteArray()))
                            .uri(URI.create(baseUrl.concat("/cas/upload")))
                            .header("Authorization", "Bearer "+accessToken)
                            .header("Content-Type", "multipart/form-data; boundary=\"" + boundary + "\"")
                            .build(), new RequestFeatureHttpAPIImpl.HttpErrorHandler<>(HttpFileUploadResponse.class))
                    .thenApply(HttpHelper.HttpOptional::getOrThrow).thenApply(response -> {
                        uploadedBlobs.put(response.storageKey(), response);
                        return response;
                    }).get();
        } catch (InterruptedException | ExecutionException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void execute(HashedDir dir, List<ProfileAction> actions) throws IOException {
        for(var action : actions) {
            execute(dir, action);
        }
    }

    public void execute(HashedDir dir, ProfileAction action) throws IOException {
        switch (action.type()) {
            case UPLOAD -> {
                if(action.source() == null) {
                    var r = dir.createParentDirectories(action.target());
                    try(var output = new ByteArrayOutputStream()) {
                        try(var input = action.input().get()) {
                            input.transferTo(output);
                        }
                        byte[] bytes = output.toByteArray();
                        var res = uploadFile(bytes);
                        HashedFile file = new HashedFile(bytes, res.url() == null ? res.storageKey() : res.url());
                        r.parent.put(r.name, file);
                    }
                } else {
                    HashedDir srcDir = new HashedDir(Path.of(action.source()), null, true, true);
                    HashedDir.Diff diff = srcDir.diff(dir, null);
                    diff.mismatch.walk("/", (HashedDir.WalkCallback) (path, name, entry) -> {
                        if(entry.getType() == HashedEntry.Type.FILE) {
                            var r = dir.createParentDirectories(Path.of(action.target()).resolve(path).toString().replace('\\', '/'));
                            try(var output = new ByteArrayOutputStream()) {
                                try(var input = IOHelper.newInput(Path.of(action.source()).resolve(path))) {
                                    input.transferTo(output);
                                }
                                byte[] bytes = output.toByteArray();
                                var res = uploadFile(bytes);
                                HashedFile file = new HashedFile(bytes, res.url() == null ? res.storageKey() : res.url());
                                r.parent.put(name, file);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        return HashedDir.WalkAction.CONTINUE;
                    });
                }
            }
            case COPY -> {
                throw new UnsupportedOperationException();
            }
            case MOVE -> {
                throw new UnsupportedOperationException();
            }
            case DELETE -> {
                throw new UnsupportedOperationException();
            }
        }
    }

    public record HttpFileUploadResponse(String hash, String algorithm, long sizeBytes, String storageKey, String url) {

    }

    public record CasPublishVersionRequest(String directoryKey, String branchName, String manifest, String metadata,
                                           Long expectedParentVersionId, Long createdBy, String message) {}
    public record CasVersionResponse(long id, long directoryId, String directoryKey, String branchName, Long parentVersionId,
                                     String manifestHash, String manifest, String metadata, String createdAt, Long createdBy, String message) {}
    public record HttpProfileMetadata(ClientProfile profile, HashedDir client, HashedDir assets) {}

    public record CasCreateDirectoryRequest(String key, String name, String metadata) {}
    public record CasDirectoryResponse(long id, String key, String name, String metadata, String createdAt) {}
    public record CasDirectoryListResponse(List<CasDirectoryResponse> profiles) {}
    public record CasProfileMetadata(UUID uuid, String name) {}

    public record HttpUncompletedProfile(UUID uuid, ClientProfile profile) implements UncompletedProfile {

        @Override
        public UUID getUuid() {
            return uuid;
        }

        @Override
        public String getName() {
            return profile.getName();
        }

        @Override
        public String getDescription() {
            return profile.getDescription();
        }

        @Override
        public String getDefaultTag() {
            return "";
        }
    }

    public record HttpProfile(ClientProfile profile, HashedDir client, HashedDir assets) implements CompletedProfile {

        @Override
        public String getTag() {
            return "";
        }

        @Override
        public ClientProfile getProfile() {
            return profile;
        }

        @Override
        public HashedDir getClientDir() {
            return client;
        }

        @Override
        public HashedDir getAssetDir() {
            return assets;
        }

        @Override
        public UUID getUuid() {
            return profile.getUUID();
        }

        @Override
        public String getName() {
            return profile.getName();
        }

        @Override
        public String getDescription() {
            return profile.getDescription();
        }

        @Override
        public String getDefaultTag() {
            return "";
        }
    }
}
