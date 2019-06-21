package pro.gravit.launcher.gui;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.LauncherAPI;
import pro.gravit.launcher.LauncherConfig;
import pro.gravit.launcher.NewLauncherSettings;
import pro.gravit.launcher.client.ClientLauncher;
import pro.gravit.launcher.client.DirBridge;
import pro.gravit.launcher.client.FunctionalBridge;
import pro.gravit.launcher.client.ServerPinger;
import pro.gravit.launcher.client.UserSettings;
import pro.gravit.launcher.hasher.FileNameMatcher;
import pro.gravit.launcher.hasher.HashedDir;
import pro.gravit.launcher.hasher.HashedEntry;
import pro.gravit.launcher.hasher.HashedFile;
import pro.gravit.launcher.managers.SettingsManager;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launcher.profiles.PlayerProfile;
import pro.gravit.launcher.profiles.Texture;
import pro.gravit.launcher.profiles.optional.OptionalFile;
import pro.gravit.launcher.request.PingRequest;
import pro.gravit.launcher.request.Request;
import pro.gravit.launcher.request.RequestException;
import pro.gravit.launcher.request.RequestType;
import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launcher.request.auth.CheckServerRequest;
import pro.gravit.launcher.request.auth.GetAvailabilityAuthRequest;
import pro.gravit.launcher.request.auth.JoinServerRequest;
import pro.gravit.launcher.request.auth.SetProfileRequest;
import pro.gravit.launcher.request.update.LauncherRequest;
import pro.gravit.launcher.request.update.ProfilesRequest;
import pro.gravit.launcher.request.update.UpdateRequest;
import pro.gravit.launcher.request.uuid.BatchProfileByUsernameRequest;
import pro.gravit.launcher.request.uuid.ProfileByUUIDRequest;
import pro.gravit.launcher.request.uuid.ProfileByUsernameRequest;
import pro.gravit.launcher.serialize.HInput;
import pro.gravit.launcher.serialize.HOutput;
import pro.gravit.launcher.serialize.signed.SignedBytesHolder;
import pro.gravit.launcher.serialize.signed.SignedObjectHolder;
import pro.gravit.launcher.serialize.stream.EnumSerializer;
import pro.gravit.launcher.serialize.stream.StreamObject;
import pro.gravit.launchergui.api.GuiEngine;
import pro.gravit.launchergui.Application;
import pro.gravit.utils.HTTPRequest;
import pro.gravit.utils.helper.CommonHelper;
import pro.gravit.utils.helper.EnvHelper;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JVMHelper;
import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.helper.SecurityHelper;
import pro.gravit.utils.helper.VerifyHelper;

public class GuiRuntimeProvider implements RuntimeProvider {

    private final GuiEngine engine = CommonHelper.newGuiEngine();
    private boolean isPreLoaded = false;

    @LauncherAPI
    public void loadConfig(String get) {
		engine.config(engine, get);
    }

    @Override
    public void run(String[] args) throws NoSuchMethodException, IOException {
        preLoad();
        LogHelper.info("Invoking start() function");
        Launcher.modulesManager.postInitModules();
        engine.start((String[]) args);
        Launcher.modulesManager.finishModules();
    }

    @Override
    public void preLoad() throws IOException {
        if (!isPreLoaded) {
            loadConfig("API");
            loadConfig("CONFIG");
            isPreLoaded = true;
        }
    }

    @Override
    public void init(boolean clientInstance) {
        loadConfig("INIT");
    }
}
