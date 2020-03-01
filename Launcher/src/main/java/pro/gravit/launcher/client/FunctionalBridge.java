package pro.gravit.launcher.client;

import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.LauncherAPI;
import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.api.AuthService;
import pro.gravit.launcher.events.request.AuthRequestEvent;
import pro.gravit.launcher.guard.LauncherGuardManager;
import pro.gravit.launcher.hasher.FileNameMatcher;
import pro.gravit.launcher.hasher.HashedDir;
import pro.gravit.launcher.hwid.HWID;
import pro.gravit.launcher.hwid.OshiHWIDProvider;
import pro.gravit.launcher.managers.ConsoleManager;
import pro.gravit.launcher.managers.GsonManager;
import pro.gravit.launcher.managers.HasherManager;
import pro.gravit.launcher.managers.HasherStore;
import pro.gravit.launcher.request.Request;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.HTTPRequest;

public class FunctionalBridge {
    public class HasteResponse {
        @LauncherNetworkAPI
        public String key;
    }
    @LauncherAPI
    public static ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(0);
    @LauncherAPI
    public static OshiHWIDProvider hwidProvider;
    @LauncherAPI
    public static AtomicReference<HWID> hwid = new AtomicReference<>();
    @LauncherAPI
    public static Thread getHWID = null;

    private static long cachedMemorySize = -1;

    @LauncherAPI
    public static HashedDirRunnable offlineUpdateRequest(String dirName, Path dir, HashedDir hdir, FileNameMatcher matcher, boolean digest) {
        return () -> {
            if (hdir == null) {
                Request.requestError(java.lang.String.format("Директории '%s' нет в кэше", dirName));
            }
            ClientLauncher.verifyHDir(dir, hdir, matcher, digest);
            return hdir;
        };
    }

    @LauncherAPI
    public static void startTask(Runnable task) {
        threadPool.execute(task);
    }

    @LauncherAPI
    public static HWID getHWID() {
        HWID hhwid = hwid.get();
        if (hhwid == null) {
            if(hwidProvider == null) hwidProvider = new OshiHWIDProvider();
            hwid.set(hwidProvider.getHWID());
        }
        return hhwid;
    }

    @LauncherAPI
    public static long getTotalMemory() {
        if (cachedMemorySize > 0) return cachedMemorySize;
        if(hwidProvider == null) hwidProvider = new OshiHWIDProvider();
        return (cachedMemorySize = hwidProvider.getTotalMemory() >> 20);
    }

    @LauncherAPI
    public static int getClientJVMBits() {
        return LauncherGuardManager.guard.getClientJVMBits();
    }

    @LauncherAPI
    public static long getJVMTotalMemory() {
        if (getClientJVMBits() == 32) {
            return Math.min(getTotalMemory(), 1536);
        } else {
            return getTotalMemory();
        }
    }

    @LauncherAPI
    public static HasherStore getDefaultHasherStore() {
        return HasherManager.getDefaultStore();
    }

    @LauncherAPI
    public static void registerUserSettings(String typename, Class<? extends UserSettings> clazz) {
        UserSettings.providers.register(typename, clazz);
    }

    @LauncherAPI
    public static void close() throws Exception {
        threadPool.awaitTermination(2, TimeUnit.SECONDS);
    }

    @LauncherAPI
    public static void setAuthParams(AuthRequestEvent event) {
        if (event.session != 0) {
            Request.setSession(event.session);
        }
        LauncherGuardManager.guard.setProtectToken(event.protectToken);
        AuthService.permissions = event.permissions;
        if(event.playerProfile != null)
        {
            AuthService.username = event.playerProfile.username;
            AuthService.uuid = event.playerProfile.uuid;
        }
    }

    @FunctionalInterface
    public interface HashedDirRunnable {
        HashedDir run() throws Exception;
    }

    @LauncherAPI
    public static void evalCommand(String cmd) {
        ConsoleManager.handler.eval(cmd, false);
    }

    @LauncherAPI
    public static void addPlainOutput(LogHelper.Output output) {
        LogHelper.addOutput(output, LogHelper.OutputTypes.PLAIN);
    }
    
    @LauncherAPI
    public static String getLauncherVersion() {
        return String.format("GravitLauncher v%d.%d.%d build %d",
            Version.MAJOR,
            Version.MINOR,
            Version.PATCH,
            Version.BUILD
        );
    }

    @LauncherAPI
    public static String hastebin(String hasteserver, String log) throws IOException {
        JsonParser parser = new JsonParser();
        Gson gson = Launcher.gsonManager.gson;

        URL url = new URL(hasteserver + "documents");
        
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "text/plain; charset=UTF-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setConnectTimeout(10000);
        try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8)) {
            writer.write(log);
            writer.flush();
        }

        InputStreamReader reader;
        int statusCode = connection.getResponseCode();

        if (200 <= statusCode && statusCode < 300)
            reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8);
        else
            reader = new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8);
        try {
            JsonElement response = parser.parse(reader);
            HasteResponse obj = gson.fromJson(response, HasteResponse.class);
            return hasteserver + obj.key;
        } catch (Exception e) {
            if (200 > statusCode || statusCode > 300) {
                LogHelper.error("JsonRequest failed. Server response code %d", statusCode);
                throw new IOException(e);
            }
            return null;
        }
    }
}
