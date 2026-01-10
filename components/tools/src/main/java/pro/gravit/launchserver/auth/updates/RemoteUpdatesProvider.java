package pro.gravit.launchserver.auth.updates;

import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.core.api.features.CoreFeatureAPI;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RemoteUpdatesProvider extends UpdatesProvider {
    public String baseUrl = "PASTE BASE URL HERE";
    public String accessToken = "PASTE ACCESS TOKEN HERE";
    private final transient HttpClient client = HttpClient.newBuilder().build();
    @Override
    public void pushUpdate(List<UpdateUploadInfo> files) throws IOException {
        for(var file : files) {
            String boundary = SecurityHelper.toHex(SecurityHelper.randomBytes(32));
            String jsonOptions = Launcher.gsonManager.gson.toJson(file.secrets());
            byte[] preFileData;
            try(ByteArrayOutputStream output = new ByteArrayOutputStream(256)) {
                output.write("--".getBytes(StandardCharsets.UTF_8));
                output.write(boundary.getBytes(StandardCharsets.UTF_8));
                output.write("\r\nContent-Disposition: form-data; name=\"secrets\"\r\nContent-Type: application/json\r\n\r\n".getBytes(StandardCharsets.UTF_8));
                output.write(jsonOptions.getBytes(StandardCharsets.UTF_8));
                output.write("\r\n--".getBytes(StandardCharsets.UTF_8));
                output.write(boundary.getBytes(StandardCharsets.UTF_8));
                output.write("\r\nContent-Disposition: form-data; name=\"file\"; filename=\"file\"\r\nContent-Type: image/png\r\n\r\n".getBytes(StandardCharsets.UTF_8));
                preFileData = output.toByteArray();
            }
            byte[] postFileData;
            try(ByteArrayOutputStream output = new ByteArrayOutputStream(128)) {
                output.write("\r\n--".getBytes(StandardCharsets.UTF_8));
                output.write(boundary.getBytes(StandardCharsets.UTF_8));
                output.write("--\r\n".getBytes(StandardCharsets.UTF_8));
                postFileData = output.toByteArray();
            }
            byte[] bytes = IOHelper.read(file.path());
            try {
                client.send(HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl.concat("/updates/upload/"+file.variant().toString())))
                        .POST(HttpRequest.BodyPublishers.concat(HttpRequest.BodyPublishers.ofByteArray(preFileData),
                                HttpRequest.BodyPublishers.ofByteArray(bytes),
                                HttpRequest.BodyPublishers.ofByteArray(postFileData)))
                        .header("Authorization", "Bearer "+accessToken)
                        .header("Content-Type", "multipart/form-data; boundary=\""+boundary+"\"")
                        .header("Accept", "application/json")
                        .build(), HttpResponse.BodyHandlers.ofByteArray());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public UpdateInfo checkUpdates(CoreFeatureAPI.UpdateVariant variant, BuildSecretsCheck buildSecretsCheck) {
        throw new UnsupportedOperationException();
    }
}
