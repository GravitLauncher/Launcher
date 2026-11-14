package pro.gravit.launchserver.distribution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.core.api.features.CoreFeatureAPI;
import pro.gravit.launchserver.helper.ProviderHttpClient;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.*;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;

public class RemoteUpdatesProvider extends UpdatesProvider {
    private static final Logger log = LoggerFactory.getLogger(RemoteUpdatesProvider.class);
    public String baseUrl = "PASTE BASE URL HERE";
    public String accessToken = "PASTE ACCESS TOKEN HERE";
    private final transient ProviderHttpClient client = new ProviderHttpClient();
    @Override
    public void pushUpdate(List<UpdateUploadInfo> files) throws IOException {
        if (files == null || files.isEmpty()) throw new IOException("At least one update artifact is required");
        for (var file : files) {
            if (file == null || file.variant() == null || file.path() == null || file.secrets() == null) {
                throw new IOException("Update artifact, variant, path, and build secrets are required");
            }
            if (!Files.isRegularFile(file.path())) throw new IOException("Update artifact is not a regular file: " + file.path());
            if (file.secrets().publicKey() == null || file.secrets().privateKey() == null) {
                throw new IOException("Update artifact build keys are required for " + file.variant());
            }
        }
        for(var file : files) {
            String boundary = SecurityHelper.toHex(SecurityHelper.randomBytes(32));
            byte[] preFileData;
            try(ByteArrayOutputStream output = new ByteArrayOutputStream(256)) {
                output.write("--".getBytes(StandardCharsets.UTF_8));
                output.write(boundary.getBytes(StandardCharsets.UTF_8));
                output.write("\r\nContent-Disposition: form-data; name=\"publicKey\"; filename=\"public.key\"\r\nContent-Type: application/octet-stream\r\n\r\n".getBytes(StandardCharsets.UTF_8));
                output.write(file.secrets().publicKey().getBytes(StandardCharsets.UTF_8));
                output.write("\r\n--".getBytes(StandardCharsets.UTF_8));
                output.write(boundary.getBytes(StandardCharsets.UTF_8));
                output.write("\r\nContent-Disposition: form-data; name=\"privateKey\"; filename=\"private.key\"\r\nContent-Type: application/octet-stream\r\n\r\n".getBytes(StandardCharsets.UTF_8));
                output.write(file.secrets().privateKey().getBytes(StandardCharsets.UTF_8));
                output.write("\r\n--".getBytes(StandardCharsets.UTF_8));
                output.write(boundary.getBytes(StandardCharsets.UTF_8));
                output.write("\r\nContent-Disposition: form-data; name=\"file\"; filename=\"file\"\r\nContent-Type: application/octet-stream\r\n\r\n".getBytes(StandardCharsets.UTF_8));
                preFileData = output.toByteArray();
            }
            byte[] postFileData;
            try(ByteArrayOutputStream output = new ByteArrayOutputStream(128)) {
                output.write("\r\n--".getBytes(StandardCharsets.UTF_8));
                output.write(boundary.getBytes(StandardCharsets.UTF_8));
                output.write("--\r\n".getBytes(StandardCharsets.UTF_8));
                postFileData = output.toByteArray();
            }
            try {
                var result = client.client().send(client.authenticatedBuilder(accessToken)
                        .timeout(Duration.ofMinutes(10))
                        .uri(client.uri(baseUrl, "/updates/upload", java.util.Map.of("variant", file.variant().toString())))
                        .POST(HttpRequest.BodyPublishers.concat(HttpRequest.BodyPublishers.ofByteArray(preFileData),
                                HttpRequest.BodyPublishers.ofFile(file.path()),
                                HttpRequest.BodyPublishers.ofByteArray(postFileData)))
                        .header("Content-Type", "multipart/form-data; boundary=\""+boundary+"\"")
                        .header("Accept", "application/json")
                        .build(), HttpResponse.BodyHandlers.ofInputStream());
                if(result.statusCode() < 200 || result.statusCode() >= 300) {
                    result.body().close();
                    throw new IOException("Failed to upload new release " + file.variant() + " (HTTP " + result.statusCode() + ")");
                } else {
                    LauncherArtifactUploaded r;
                    try(Reader reader = new InputStreamReader(result.body(), StandardCharsets.UTF_8)) {
                        r = Launcher.gsonManager.gson.fromJson(reader, LauncherArtifactUploaded.class);
                    }
                    if (r == null || r.url() == null) throw new IOException("Update service returned no artifact URL for " + file.variant());
                    log.info("Uploaded release {}: {}", file.variant(), r.url());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while uploading release " + file.variant(), e);
            }
        }
    }

    @Override
    public UpdateInfo checkUpdates(CoreFeatureAPI.UpdateVariant variant, BuildSecretsCheck buildSecretsCheck) {
        throw new UnsupportedOperationException("Remote updates provider does not expose update checking");
    }

    @Override
    public void close() {
        client.close();
    }

    public record LauncherArtifactUploaded(String url) {

    }
}
