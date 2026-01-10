package pro.gravit.launchserver.auth.profiles;

import pro.gravit.launcher.base.helper.HttpHelper;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launcher.base.request.RequestFeatureHttpAPIImpl;
import pro.gravit.launcher.core.hasher.HashedDir;
import pro.gravit.launcher.core.hasher.HashedEntry;
import pro.gravit.launcher.core.hasher.HashedFile;
import pro.gravit.utils.helper.IOHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class RemoteProfilesProvider extends ProfilesProvider {
    public String baseUrl = "PASTE BASE URL HERE";
    public String accessToken = "PASTE ACCESS TOKEN HERE";
    private final transient HttpClient client = HttpClient.newBuilder().build();
    @Override
    public UncompletedProfile create(String name, String description, CompletedProfile basic) {
        try {
            return HttpHelper.sendAsync(client, HttpRequest.newBuilder()
                            .POST(HttpHelper.jsonBodyPublisher(new HttpCreateProfileRequest(name, description, basic.getProfile())))
                            .uri(URI.create(baseUrl.concat("/profile/new")))
                            .header("Authorization", "Bearer "+accessToken)
                            .build(), new RequestFeatureHttpAPIImpl.HttpErrorHandler<>(HttpUncompletedProfile.class))
                    .thenApply(HttpHelper.HttpOptional::getOrThrow).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(UncompletedProfile profile) {
        try {
            HttpHelper.sendAsync(client, HttpRequest.newBuilder()
                    .DELETE()
                    .uri(URI.create(baseUrl.concat("/profile/"+profile.getUuid())))
                    .header("Authorization", "Bearer "+accessToken)
                    .build(), new RequestFeatureHttpAPIImpl.HttpErrorHandler<>(Void.class))
                    .thenApply(HttpHelper.HttpOptional::getOrThrow).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<UncompletedProfile> getProfiles() {
        try {
            return HttpHelper.sendAsync(client, HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(baseUrl.concat("/profile/list")))
                    .header("Authorization", "Bearer "+accessToken)
                    .build(), new RequestFeatureHttpAPIImpl.HttpErrorHandler<>(RequestFeatureHttpAPIImpl.HttpListProfilesResponse.class))
                    .thenApply(e -> e.getOrThrow().profiles().stream()
                            .map(HttpUncompletedProfile::new)
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
        HashedDir clientDir = prev.getClientDir();
        HashedDir assetDir = prev.getAssetDir();
        if(flags.contains(UpdateFlag.USE_DEFAULT_ASSETS)) {
            assetDir = getUnconnectedDirectory("assets");
        }
        if(assetActions != null) {
            execute(assetDir, assetActions);
        }
        if(clientActions != null) {
            execute(clientDir, clientActions);
        }

        try {
            return HttpHelper.sendAsync(client, HttpRequest.newBuilder()
                            .POST(HttpHelper.jsonBodyPublisher(new HttpUpdateProfileRequest(clientProfile, clientDir, assetDir)))
                            .uri(URI.create(baseUrl.concat("/profile/by/uuid/"+profile.getUuid()+"/pushupdate")))
                            .header("Authorization", "Bearer "+accessToken)
                            .build(), new RequestFeatureHttpAPIImpl.HttpErrorHandler<>(HttpProfile.class))
                    .thenApply(HttpHelper.HttpOptional::getOrThrow).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void download(CompletedProfile profile, Map<String, Path> files, boolean assets) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public HashedDir getUnconnectedDirectory(String name) {
        try {
            return HttpHelper.sendAsync(client, HttpRequest.newBuilder()
                            .GET()
                            .uri(URI.create(baseUrl.concat("/profile/unconnected/"+name)))
                            .header("Authorization", "Bearer "+accessToken)
                            .build(), new RequestFeatureHttpAPIImpl.HttpErrorHandler<>(HashedDir.class))
                    .thenApply(HttpHelper.HttpOptional::getOrThrow)
                    .get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompletedProfile get(UUID uuid, String tag) {
        try {
            return HttpHelper.sendAsync(client, HttpRequest.newBuilder()
                            .GET()
                            .uri(URI.create(baseUrl.concat("/profile/by/uuid/"+uuid)))
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
                            .uri(URI.create(baseUrl.concat("/profile/by/name/"+name)))
                            .header("Authorization", "Bearer "+accessToken)
                            .build(), new RequestFeatureHttpAPIImpl.HttpErrorHandler<>(HttpProfile.class))
                    .thenApply(HttpHelper.HttpOptional::getOrThrow)
                    .get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public HttpFileUploadResponse uploadFile(HttpRequest.BodyPublisher bodyPublisher) {
        try {
            return HttpHelper.sendAsync(client, HttpRequest.newBuilder()
                            .POST(bodyPublisher)
                            .uri(URI.create(baseUrl.concat("/profile/uploadfile")))
                            .header("Authorization", "Bearer "+accessToken)
                            .build(), new RequestFeatureHttpAPIImpl.HttpErrorHandler<>(HttpFileUploadResponse.class))
                    .thenApply(HttpHelper.HttpOptional::getOrThrow).get();
        } catch (InterruptedException | ExecutionException e) {
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
                        var res = uploadFile(HttpRequest.BodyPublishers.ofByteArray(bytes));
                        HashedFile file = new HashedFile(bytes, res.url());
                        r.parent.put(r.name, file);
                    }
                } else {
                    HashedDir srcDir = new HashedDir(Path.of(action.source()), null, true, true);
                    HashedDir.Diff diff = dir.diff(srcDir, null);
                    diff.mismatch.walk("/", (HashedDir.WalkCallback) (path, name, entry) -> {
                        if(entry.getType() == HashedEntry.Type.FILE) {
                            var r = dir.createParentDirectories(action.target());
                            try(var output = new ByteArrayOutputStream()) {
                                try(var input = IOHelper.newInput(Path.of(action.source()).resolve(path))) {
                                    input.transferTo(output);
                                }
                                byte[] bytes = output.toByteArray();
                                var res = uploadFile(HttpRequest.BodyPublishers.ofByteArray(bytes));
                                HashedFile file = new HashedFile(bytes, res.url());
                                r.parent.put(r.name, file);
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

    public record HttpFileUploadResponse(String url) {

    }

    public record HttpCreateProfileRequest(String name, String description, ClientProfile profile) {

    }

    public record HttpUpdateProfileRequest(ClientProfile profile, HashedDir clientDir, HashedDir assetDir) {

    }

    public record HttpUncompletedProfile(ClientProfile profile) implements UncompletedProfile {

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

    public record HttpProfile(ClientProfile profile, HashedDir clientDir, HashedDir assetDir) implements CompletedProfile {

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
            return clientDir;
        }

        @Override
        public HashedDir getAssetDir() {
            return assetDir;
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
