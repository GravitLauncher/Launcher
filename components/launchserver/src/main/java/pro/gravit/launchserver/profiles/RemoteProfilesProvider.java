package pro.gravit.launchserver.profiles;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
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
import pro.gravit.launchserver.helper.ProviderHttpClient;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.SecurityHelper;
import pro.gravit.utils.helper.ManagedPath;

import java.io.FileNotFoundException;
import java.io.IOException;
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
    private final transient ProviderHttpClient client = new ProviderHttpClient();
    private CasHttpClient cas() { return new CasHttpClient(client, baseUrl, accessToken); }
    private final transient Map<String, HttpFileUploadResponse> uploadedBlobs = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public Capabilities capabilities() {
        return new Capabilities(EnumSet.of(ProfileAction.ProfileActionType.UPLOAD), false);
    }
    @Override
    public UncompletedProfile create(String name, String description, CompletedProfile basic) {
        try {
            UUID uuid = UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return cas().sendAsync(cas().authorizedBuilder()
                            .POST(HttpHelper.jsonBodyPublisher(new CasCreateDirectoryRequest(uuid.toString(), name,
                                    Launcher.gsonManager.gson.toJson(new CasProfileMetadata(uuid, name)))))
                            .uri(cas().uri("/cas/directories", Map.of()))
                            .header("Content-Type", "application/json")
                            .build(), new RequestFeatureHttpAPIImpl.HttpErrorHandler<>(CasDirectoryResponse.class))
                    .thenApply(HttpHelper.HttpOptional::getOrThrow).thenCompose(result -> {
                        CasProfileMetadata metadata = Launcher.gsonManager.gson.fromJson(result.metadata(), CasProfileMetadata.class);
                        UUID profileUuid = metadata.uuid();
                        ClientProfile newClientProfile = basic == null ? new ClientProfileBuilder()
                                .setTitle(name).setInfo(description).setDir(name).setUuid(profileUuid).createClientProfile()
                                : new ClientProfileBuilder(basic.getProfile()).setTitle(name).setInfo(description).setDir(name).setUuid(profileUuid).createClientProfile();
                        if (basic == null) {
                            return pushUpdateAsync(new HttpUncompletedProfile(profileUuid, newClientProfile), newClientProfile, null, null)
                                    .thenApply(e -> (UncompletedProfile) new HttpUncompletedProfile(profileUuid, newClientProfile));
                        }
                        return CompletableFuture.completedFuture((UncompletedProfile) new HttpUncompletedProfile(profileUuid, newClientProfile));
                    }).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while creating CAS profile", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Failed to create CAS profile", e.getCause() == null ? e : e.getCause());
        }
    }

    @Override
    public void delete(UncompletedProfile profile) {
        throw new UnsupportedOperationException("Remote CAS provider cannot delete directories; the configured CAS API has no delete endpoint");
    }

    @Override
    public Set<UncompletedProfile> getProfiles() {
        try {
            return cas().sendAsync(cas().authorizedBuilder()
                            .GET()
                            .uri(cas().uri("/cas/directories/list", Map.of()))

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
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while listing CAS profiles", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Failed to list CAS profiles", e.getCause() == null ? e : e.getCause());
        }
    }

    @Override
    public CompletedProfile pushUpdate(UncompletedProfile profile, String tag, ClientProfile clientProfile, List<ProfileAction> assetActions, List<ProfileAction> clientActions, List<UpdateFlag> flags) throws IOException {
        requireSupportedRevision(tag);
        validateActions(assetActions);
        validateActions(clientActions);
        var prev = get(profile.getUuid(), tag);
        if(prev != null && prev.getProfile() != null && (prev.getProfile().getUUID() == null || !prev.getProfile().getUUID().equals(prev.getUuid()))) {
            clientProfile = new ClientProfileBuilder(clientProfile)
                    .setUuid(prev.getUuid())
                    .createClientProfile();
        }
        HashedDir clientDir = prev == null ? null : prev.getClientDir();
        HashedDir assetDir = prev == null ? null : prev.getAssetDir();
        if(flags != null && flags.contains(UpdateFlag.USE_DEFAULT_ASSETS)) {
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
            CompletedProfile published = pushUpdateAsync(profile, clientProfile, clientDir, assetDir).get();
            deleteSourcesAfterPublish(assetActions, flags != null && flags.contains(UpdateFlag.DELETE_SOURCE_FILES));
            deleteSourcesAfterPublish(clientActions, flags != null && flags.contains(UpdateFlag.DELETE_SOURCE_FILES));
            return published;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while publishing CAS profile version", e);
        } catch (ExecutionException e) {
            throw new IOException("Failed to publish CAS profile version", e.getCause() == null ? e : e.getCause());
        }
    }

    private void validateActions(List<ProfileAction> actions) throws IOException {
        if (actions == null) return;
        requireSupportedActions(actions);
        for (ProfileAction action : actions) {
            if (action.target() == null || action.target().isBlank()) {
                if (action.source() == null) throw new IOException("Stream uploads require a target path");
            } else {
                validateTarget(action.target());
            }
            if (action.source() == null && action.input() == null) {
                throw new IOException("Upload action requires either a source path or input stream");
            }
            if (action.source() != null) {
                Path source = Path.of(action.source());
                if (!java.nio.file.Files.exists(source) || !java.nio.file.Files.isReadable(source)) {
                    throw new IOException("Upload source is missing or unreadable: " + source);
                }
                if (!java.nio.file.Files.isDirectory(source) && !java.nio.file.Files.isRegularFile(source)) {
                    throw new IOException("Upload source must be a regular file or directory: " + source);
                }
                if ((action.target() == null || action.target().isBlank()) && !java.nio.file.Files.isDirectory(source)) {
                    throw new IOException("A file upload requires a file name target");
                }
                try (var paths = java.nio.file.Files.walk(source)) {
                    var iterator = paths.iterator();
                    while (iterator.hasNext()) {
                        Path item = iterator.next();
                        if (java.nio.file.Files.isSymbolicLink(item)) throw new IOException("Symbolic links are not allowed in upload sources: " + item);
                        if (!java.nio.file.Files.isReadable(item)) throw new IOException("Upload source contains an unreadable path: " + item);
                        if (!java.nio.file.Files.isDirectory(item) && !java.nio.file.Files.isRegularFile(item)) {
                            throw new IOException("Upload source contains a non-regular file: " + item);
                        }
                    }
                }
            }
        }
    }

    private static void validateTarget(String target) throws IOException {
        if (target == null || target.isBlank()) throw new IOException("Profile action target must not be empty");
        Path path = Path.of(target);
        boolean parentComponent = false;
        for (Path part : path) parentComponent |= part.toString().equals("..");
        if (path.isAbsolute() || parentComponent || path.normalize().toString().equals(".")) {
            throw new IOException("Profile action target must remain inside its managed directory: " + target);
        }
    }

    private static void deleteSourcesAfterPublish(List<ProfileAction> actions, boolean deleteAllSources) throws IOException {
        if (actions == null) return;
        for (ProfileAction action : actions) {
            if (action.type() == ProfileAction.ProfileActionType.UPLOAD && (action.deleteSource() || deleteAllSources) && action.source() != null) {
                Path source = Path.of(action.source());
                if (java.nio.file.Files.isSymbolicLink(source)) throw new IOException("Refusing to delete a symbolic link upload source: " + source);
                if (java.nio.file.Files.isDirectory(source)) IOHelper.deleteDir(source, true);
                else java.nio.file.Files.deleteIfExists(source);
            }
        }
    }

    private CompletableFuture<HttpProfile> pushUpdateAsync(UncompletedProfile profile, ClientProfile clientProfile, HashedDir clientDir, HashedDir assetDir) {
        JsonArray files = new JsonArray();
        appendManifest(files, "client", clientDir);
        appendManifest(files, "assets", assetDir);
        JsonObject manifestObject = new JsonObject();
        manifestObject.add("files", files);
        return cas().sendAsync(cas().authorizedBuilder()
                        .POST(HttpHelper.jsonBodyPublisher(new CasPublishVersionRequest(profile.getUuid().toString(), "main", Launcher.gsonManager.gson.toJson(manifestObject),
                                Launcher.gsonManager.gson.toJson(new HttpProfileMetadata(clientProfile, clientDir, assetDir)), null, null, null)))
                        .uri(cas().uri("/cas/versions", Map.of()))
                        .header("Content-Type", "application/json")

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
        Map<Path, List<Downloader.SizedFile>> downloadsByRoot = new LinkedHashMap<>();
        for(var e : files.entrySet()) {
            var key = e.getKey();
            var path = e.getValue();
            if(!key.isEmpty() && !key.equals(".")) {
                validateTarget(key);
                var ref = dir.tryFindRecursive(key);
                if(!ref.isFound()) {
                    throw new FileNotFoundException(key);
                }
                if(ref.entry instanceof HashedFile file) {
                    addDownload(downloadsByRoot, file, path);
                } else if(ref.entry instanceof HashedDir hdir) {
                    hdir.walk("/", (rpath, name, entry) -> {
                        if(entry instanceof HashedFile file) {
                            try {
                                addDownload(downloadsByRoot, file, ManagedPath.resolve(path, rpath));
                            } catch (IOException ex) {
                                throw new java.io.UncheckedIOException(ex);
                            }
                        }
                        return HashedDir.WalkAction.CONTINUE;
                    });
                }
            } else {
                dir.walk("/", (rpath, name, entry) -> {
                    if(entry instanceof HashedFile file) {
                        try {
                            addDownload(downloadsByRoot, file, ManagedPath.resolve(path, rpath));
                        } catch (IOException ex) {
                            throw new java.io.UncheckedIOException(ex);
                        }
                    }
                    return HashedDir.WalkAction.CONTINUE;
                });
            }
        }
        long count = downloadsByRoot.values().stream().mapToLong(List::size).sum();
        log.info("Download {} files", count);
        try {
            for (var entry : downloadsByRoot.entrySet()) {
                Downloader downloader = Command.downloadWithProgressBar("files", entry.getValue(), "https://example.com", entry.getKey());
                try {
                    downloader.getFuture().get();
                } catch (InterruptedException e) {
                    downloader.cancel();
                    throw e;
                } catch (java.util.concurrent.ExecutionException e) {
                    downloader.cancel();
                    throw e;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while downloading CAS profile files", e);
        } catch (java.util.concurrent.ExecutionException e) {
            throw new IOException("Failed to download CAS profile files", e.getCause() == null ? e : e.getCause());
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private static void addDownload(Map<Path, List<Downloader.SizedFile>> downloadsByRoot, HashedFile file, Path target) throws IOException {
        Path absolute = target.toAbsolutePath().normalize();
        Path parent = absolute.getParent();
        if (parent == null || absolute.getFileName() == null) throw new IOException("Invalid profile download destination: " + target);
        ManagedPath.resolve(parent, absolute.getFileName().toString());
        IOHelper.createParentDirs(absolute);
        downloadsByRoot.computeIfAbsent(parent, ignored -> new ArrayList<>())
                .add(new Downloader.SizedFile(file.url, absolute.getFileName().toString(), file.size()));
    }

    @Override
    public HashedDir getUnconnectedDirectory(String name) {
        try {
            return cas().sendAsync(cas().authorizedBuilder()
                            .GET()
                            .uri(cas().uri("/cas/versions/latest", Map.of("directoryKey", name, "branchName", "main")))

                            .build(), new RequestFeatureHttpAPIImpl.HttpErrorHandler<>(HashedDir.class))
                    .thenApply(response -> response.statusCode() == 404 ? null : response.getOrThrow())
                    .get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while reading CAS directory", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Failed to read CAS directory '" + name + "'", e.getCause() == null ? e : e.getCause());
        }
    }

    @Override
    public CompletedProfile get(UUID uuid, String tag) {
        requireSupportedRevision(tag);
        try {
            return cas().sendAsync(cas().authorizedBuilder()
                            .GET()
                            .uri(cas().uri("/cas/versions/latest", Map.of("directoryKey", uuid.toString(), "branchName", "main")))

                            .build(), new RequestFeatureHttpAPIImpl.HttpErrorHandler<>(HttpProfile.class))
                    .thenApply(response -> response.statusCode() == 404 ? null : response.getOrThrow())
                    .get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while reading CAS profile", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Failed to read CAS profile", e.getCause() == null ? e : e.getCause());
        }
    }

    @Override
    public CompletedProfile get(String name, String tag) {
        requireSupportedRevision(tag);
        for (UncompletedProfile profile : getProfiles()) {
            if (Objects.equals(profile.getName(), name)) return get(profile.getUuid(), tag);
        }
        try {
            return cas().sendAsync(cas().authorizedBuilder()
                            .GET()
                            .uri(cas().uri("/cas/versions/latest", Map.of("directoryKey", name, "branchName", "main")))

                            .build(), new RequestFeatureHttpAPIImpl.HttpErrorHandler<>(HttpProfile.class))
                    .thenApply(response -> response.statusCode() == 404 ? null : response.getOrThrow())
                    .get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while reading CAS profile", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Failed to read CAS profile", e.getCause() == null ? e : e.getCause());
        }
    }

    private HttpFileUploadResponse uploadFile(byte[] bytes) {
        try {
            String boundary = SecurityHelper.toHex(SecurityHelper.randomBytes(16));
            HttpRequest.BodyPublisher multipart = HttpRequest.BodyPublishers.concat(
                    HttpRequest.BodyPublishers.ofByteArray(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"file\"\r\nContent-Type: application/octet-stream\r\n\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                    HttpRequest.BodyPublishers.ofByteArray(bytes),
                    HttpRequest.BodyPublishers.ofByteArray(("\r\n--" + boundary + "--\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            return cas().sendAsync(cas().authorizedBuilder()
                            .POST(multipart)
                            .uri(cas().uri("/cas/upload", Map.of()))
                            .timeout(java.time.Duration.ofMinutes(10))

                            .header("Content-Type", "multipart/form-data; boundary=\"" + boundary + "\"")
                            .build(), new RequestFeatureHttpAPIImpl.HttpErrorHandler<>(HttpFileUploadResponse.class))
                    .thenApply(HttpHelper.HttpOptional::getOrThrow).thenApply(response -> {
                        registerBlob(response);
                        return response;
                    }).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while uploading CAS blob", e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private HttpFileUploadResponse uploadFile(Path path) {
        try {
            String boundary = SecurityHelper.toHex(SecurityHelper.randomBytes(16));
            HttpRequest.BodyPublisher multipart = HttpRequest.BodyPublishers.concat(
                    HttpRequest.BodyPublishers.ofByteArray(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"file\"\r\nContent-Type: application/octet-stream\r\n\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                    HttpRequest.BodyPublishers.ofFile(path),
                    HttpRequest.BodyPublishers.ofByteArray(("\r\n--" + boundary + "--\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            return cas().sendAsync(cas().authorizedBuilder().POST(multipart)
                            .uri(cas().uri("/cas/upload", Map.of()))
                            .timeout(java.time.Duration.ofMinutes(10))
                            .header("Content-Type", "multipart/form-data; boundary=\"" + boundary + "\"")
                            .build(), new RequestFeatureHttpAPIImpl.HttpErrorHandler<>(HttpFileUploadResponse.class))
                    .thenApply(HttpHelper.HttpOptional::getOrThrow).thenApply(response -> {
                        registerBlob(response);
                        return response;
                    }).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while uploading CAS blob", e);
        } catch (ExecutionException | IOException e) {
            throw new IllegalStateException("Failed to upload CAS blob", e);
        }
    }

    private void registerBlob(HttpFileUploadResponse response) {
        uploadedBlobs.put(response.storageKey(), response);
        if (response.url() != null) uploadedBlobs.put(response.url(), response);
    }

    private List<HttpFileUploadResponse> uploadFiles(List<byte[]> files) {
        List<HttpFileUploadResponse> responses = new ArrayList<>(files.size());
        int offset = 0;
        while (offset < files.size()) {
            int end = offset;
            long batchBytes = 0;
            while (end < files.size() && end - offset < 10) {
                long nextSize = files.get(end).length;
                if (end > offset && batchBytes + nextSize > 32L * 1024 * 1024) break;
                batchBytes += nextSize;
                end++;
            }
            List<byte[]> batch = files.subList(offset, end);
            if (batch.size() == 1) {
                responses.add(uploadFile(batch.get(0)));
            } else {
                responses.addAll(uploadBulkFiles(batch));
            }
            offset = end;
        }
        return responses;
    }

    private List<HttpFileUploadResponse> uploadBulkFiles(List<byte[]> files) {
        try {
            String boundary = SecurityHelper.toHex(SecurityHelper.randomBytes(16));
            List<HttpRequest.BodyPublisher> parts = new ArrayList<>(files.size() * 3 + 1);
            for (byte[] bytes : files) {
                parts.add(HttpRequest.BodyPublishers.ofByteArray(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"files\"; filename=\"file\"\r\nContent-Type: application/octet-stream\r\n\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                parts.add(HttpRequest.BodyPublishers.ofByteArray(bytes));
                parts.add(HttpRequest.BodyPublishers.ofByteArray("\r\n".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            }
            parts.add(HttpRequest.BodyPublishers.ofByteArray(("--" + boundary + "--\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            return cas().sendAsync(cas().authorizedBuilder()
                            .POST(HttpRequest.BodyPublishers.concat(parts.toArray(HttpRequest.BodyPublisher[]::new)))
                            .uri(cas().uri("/cas/upload/bulk", Map.of()))
                            .timeout(java.time.Duration.ofMinutes(10))

                            .header("Content-Type", "multipart/form-data; boundary=\"" + boundary + "\"")
                            .build(), new RequestFeatureHttpAPIImpl.HttpErrorHandler<JsonArray>(JsonArray.class))
                    .thenApply(optional -> optional.getOrThrow())
                    .thenApply(json -> Launcher.gsonManager.gson.<List<HttpFileUploadResponse>>fromJson(json,
                            new TypeToken<List<HttpFileUploadResponse>>() {}.getType()))
                    .thenApply(result -> {
                        if (result.size() != files.size()) {
                            throw new IllegalStateException("File upload returned " + result.size()
                                    + " responses for " + files.size() + " files");
                        }
                        result.forEach(this::registerBlob);
                        return result;
                    }).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while uploading CAS blobs", e);
        } catch (ExecutionException e) {
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
                    Path temporary = java.nio.file.Files.createTempFile("launchserver-cas-upload-", ".tmp");
                    try {
                        try (var input = action.input().get()) {
                            if (input == null) throw new IOException("Upload input supplier returned null");
                            java.nio.file.Files.copy(input, temporary, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        }
                        long size = java.nio.file.Files.size(temporary);
                        HttpFileUploadResponse response = uploadFile(temporary);
                        String url = response.url() == null ? response.storageKey() : response.url();
                        HashedFile file = new HashedFile(size, SecurityHelper.digest(HashedFile.DIGEST_ALGO, temporary), url);
                        r.parent.put(r.name, file);
                    } finally {
                        java.nio.file.Files.deleteIfExists(temporary);
                    }
                } else {
                    Path sourceRoot = Path.of(action.source()).toAbsolutePath().normalize();
                    HashedDir srcDir = new HashedDir(sourceRoot, null, true, true);
                    HashedDir.Diff diff = srcDir.diff(dir, null);
                    List<PendingUpload> pending = new ArrayList<>();
                    diff.mismatch.walk("/", (HashedDir.WalkCallback) (path, name, entry) -> {
                        if(entry.getType() == HashedEntry.Type.FILE) {
                            pending.add(new PendingUpload(path, name, entry.size()));
                        }
                        return HashedDir.WalkAction.CONTINUE;
                    });
                    int offset = 0;
                    while (offset < pending.size()) {
                        int end = offset;
                        long totalBytes = 0;
                        List<byte[]> batch = new ArrayList<>(10);
                        if (pending.get(offset).size() > 32L * 1024 * 1024) {
                            PendingUpload upload = pending.get(offset);
                            Path source = ManagedPath.resolve(sourceRoot, upload.path());
                            Path snapshot = java.nio.file.Files.createTempFile("launchserver-cas-upload-", ".tmp");
                            try {
                                java.nio.file.Files.copy(source, snapshot, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                                if (java.nio.file.Files.size(snapshot) != upload.size()) throw new IOException("Upload source changed while being prepared: " + source);
                                HttpFileUploadResponse response = uploadFile(snapshot);
                                String url = response.url() == null ? response.storageKey() : response.url();
                                var ref = dir.createParentDirectories(Path.of(action.target()).resolve(upload.path()).toString().replace('\\', '/'));
                                ref.parent.put(upload.name(), new HashedFile(upload.size(), SecurityHelper.digest(HashedFile.DIGEST_ALGO, snapshot), url));
                            } finally {
                                java.nio.file.Files.deleteIfExists(snapshot);
                            }
                            offset++;
                            continue;
                        }
                        while (end < pending.size() && end - offset < 10) {
                            PendingUpload upload = pending.get(end);
                            if (end > offset && totalBytes + upload.size() > 32L * 1024 * 1024) break;
                            Path source = ManagedPath.resolve(sourceRoot, upload.path());
                            byte[] bytes = IOHelper.read(source);
                            batch.add(bytes);
                            totalBytes += bytes.length;
                            end++;
                        }
                        List<HttpFileUploadResponse> responses = uploadFiles(batch);
                        if (responses.size() != batch.size()) throw new IOException("File upload returned an unexpected number of responses");
                        for (int i = 0; i < batch.size(); i++) {
                            PendingUpload upload = pending.get(offset + i);
                            var ref = dir.createParentDirectories(Path.of(action.target()).resolve(upload.path()).toString().replace('\\', '/'));
                            var response = responses.get(i);
                            ref.parent.put(upload.name(), new HashedFile(batch.get(i), response.url() == null ? response.storageKey() : response.url()));
                        }
                        offset = end;
                    }
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

    private record PendingUpload(String path, String name, long size) {}

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

    @Override
    public void close() {
        client.close();
    }
}
