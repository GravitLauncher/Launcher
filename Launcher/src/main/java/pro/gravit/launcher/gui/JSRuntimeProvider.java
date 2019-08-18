package pro.gravit.launcher.gui;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import pro.gravit.launcher.JSApplication;
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
import pro.gravit.launcher.hwid.NoHWID;
import pro.gravit.launcher.hwid.OshiHWID;
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
import pro.gravit.launcher.serialize.stream.EnumSerializer;
import pro.gravit.launcher.serialize.stream.StreamObject;
import pro.gravit.utils.HTTPRequest;
import pro.gravit.utils.helper.CommonHelper;
import pro.gravit.utils.helper.EnvHelper;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JVMHelper;
import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.helper.SecurityHelper;
import pro.gravit.utils.helper.VerifyHelper;

public class JSRuntimeProvider implements RuntimeProvider {

	public final ScriptEngine engine = CommonHelper.newScriptEngine();
    private boolean isPreLoaded = false;

    @LauncherAPI
    public static void addLauncherClassBindings(Map<String, Object> bindings) {
        bindings.put("LauncherClass", Launcher.class);
        bindings.put("LauncherConfigClass", LauncherConfig.class);
        bindings.put("HTTPRequestClass", HTTPRequest.class);
        bindings.put("SettingsManagerClass", SettingsManager.class);
        bindings.put("NewLauncherSettingsClass", NewLauncherSettings.class);

        // Set client class bindings
        bindings.put("PlayerProfileClass", PlayerProfile.class);
        bindings.put("PlayerProfileTextureClass", Texture.class);
        bindings.put("ClientProfileClass", ClientProfile.class);
        bindings.put("ClientProfileVersionClass", ClientProfile.Version.class);
        bindings.put("ClientLauncherClass", ClientLauncher.class);
        bindings.put("ClientLauncherParamsClass", ClientLauncher.Params.class);
        bindings.put("ServerPingerClass", ServerPinger.class);

        // Set request class bindings
        bindings.put("RequestClass", Request.class);
        bindings.put("RequestTypeClass", RequestType.class);
        bindings.put("RequestExceptionClass", RequestException.class);
        bindings.put("PingRequestClass", PingRequest.class);
        bindings.put("AuthRequestClass", AuthRequest.class);
        bindings.put("JoinServerRequestClass", JoinServerRequest.class);
        bindings.put("CheckServerRequestClass", CheckServerRequest.class);
        bindings.put("UpdateRequestClass", UpdateRequest.class);
        bindings.put("LauncherRequestClass", LauncherRequest.class);
        bindings.put("SetProfileRequestClass", SetProfileRequest.class);
        bindings.put("ProfilesRequestClass", ProfilesRequest.class);
        bindings.put("ProfileByUsernameRequestClass", ProfileByUsernameRequest.class);
        bindings.put("ProfileByUUIDRequestClass", ProfileByUUIDRequest.class);
        bindings.put("BatchProfileByUsernameRequestClass", BatchProfileByUsernameRequest.class);
        bindings.put("GetAvailabilityAuthRequestClass", GetAvailabilityAuthRequest.class);

        // Set serialization class bindings
        bindings.put("HInputClass", HInput.class);
        bindings.put("HOutputClass", HOutput.class);
        bindings.put("StreamObjectClass", StreamObject.class);
        bindings.put("StreamObjectAdapterClass", StreamObject.Adapter.class);
        bindings.put("EnumSerializerClass", EnumSerializer.class);
        bindings.put("OptionalFileClass", OptionalFile.class);
        bindings.put("UserSettingsClass", UserSettings.class);

        // Set helper class bindings
        bindings.put("CommonHelperClass", CommonHelper.class);
        bindings.put("IOHelperClass", IOHelper.class);
        bindings.put("EnvHelperClass", EnvHelper.class);
        bindings.put("JVMHelperClass", JVMHelper.class);
        bindings.put("JVMHelperOSClass", JVMHelper.OS.class);
        bindings.put("LogHelperClass", LogHelper.class);
        bindings.put("LogHelperOutputClass", LogHelper.Output.class);
        bindings.put("SecurityHelperClass", SecurityHelper.class);
        bindings.put("DigestAlgorithmClass", SecurityHelper.DigestAlgorithm.class);
        bindings.put("VerifyHelperClass", VerifyHelper.class);
        bindings.put("DirBridgeClass", DirBridge.class);
        bindings.put("FunctionalBridgeClass", FunctionalBridge.class);

        bindings.put("NoHWIDClass", NoHWID.class);
        bindings.put("OshiHWIDClass", OshiHWID.class);

        // Load JS API if available
        try {
            Class.forName("javafx.application.Application");
            bindings.put("JSApplicationClass", JSApplication.class);
        } catch (ClassNotFoundException ignored) {
            LogHelper.warning("JavaFX API isn't available");
        }
    }

    @LauncherAPI
    public Object loadScript(String path) throws IOException, ScriptException {
        URL url = Launcher.getResourceURL(path);
        LogHelper.debug("Loading script: '%s'", url);
        try (BufferedReader reader = IOHelper.newReader(url)) {
            return engine.eval(reader, engine.getBindings(ScriptContext.ENGINE_SCOPE));
        }
    }

    private void setScriptBindings() {
        LogHelper.info("Setting up script engine bindings");
        Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.put("launcher", this);

        // Add launcher class bindings
        JSRuntimeProvider.addLauncherClassBindings(bindings);
    }

    @Override
    public void run(String[] args) throws ScriptException, NoSuchMethodException, IOException {
        preLoad();
        loadScript(Launcher.INIT_SCRIPT_FILE);
        LogHelper.info("Invoking start() function");
        Launcher.modulesManager.postInitModules();
        ((Invocable) engine).invokeFunction("start", (Object) args);
        Launcher.modulesManager.finishModules();
    }

    @Override
    public void preLoad() throws IOException, ScriptException {
        if (!isPreLoaded) {
            loadScript(Launcher.API_SCRIPT_FILE);
            loadScript(Launcher.CONFIG_SCRIPT_FILE);
            isPreLoaded = true;
        }
    }

    @Override
    public void init(boolean clientInstance) {
        setScriptBindings();
    }
}
