package ru.gravit.launcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import ru.gravit.launcher.client.*;
import ru.gravit.launcher.gui.buttons.RingProgressIndicator;
import ru.gravit.launcher.gui.buttons.RingProgressIndicatorSkin;
import ru.gravit.launcher.hasher.FileNameMatcher;
import ru.gravit.launcher.hasher.HashedDir;
import ru.gravit.launcher.hasher.HashedEntry;
import ru.gravit.launcher.hasher.HashedFile;
import ru.gravit.utils.HTTPRequest;
import ru.gravit.utils.helper.CommonHelper;
import ru.gravit.utils.helper.EnvHelper;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.JVMHelper;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.utils.helper.SecurityHelper;
import ru.gravit.utils.helper.VerifyHelper;
import ru.gravit.launcher.profiles.ClientProfile;
import ru.gravit.launcher.profiles.PlayerProfile;
import ru.gravit.launcher.profiles.Texture;
import ru.gravit.launcher.request.CustomRequest;
import ru.gravit.launcher.request.PingRequest;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.RequestException;
import ru.gravit.launcher.request.RequestType;
import ru.gravit.launcher.request.auth.AuthRequest;
import ru.gravit.launcher.request.auth.CheckServerRequest;
import ru.gravit.launcher.request.auth.JoinServerRequest;
import ru.gravit.launcher.request.update.LauncherRequest;
import ru.gravit.launcher.request.update.UpdateRequest;
import ru.gravit.launcher.request.uuid.BatchProfileByUsernameRequest;
import ru.gravit.launcher.request.uuid.ProfileByUUIDRequest;
import ru.gravit.launcher.request.uuid.ProfileByUsernameRequest;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.config.ConfigObject;
import ru.gravit.launcher.serialize.config.TextConfigReader;
import ru.gravit.launcher.serialize.config.TextConfigWriter;
import ru.gravit.launcher.serialize.config.entry.BlockConfigEntry;
import ru.gravit.launcher.serialize.config.entry.BooleanConfigEntry;
import ru.gravit.launcher.serialize.config.entry.ConfigEntry;
import ru.gravit.launcher.serialize.config.entry.IntegerConfigEntry;
import ru.gravit.launcher.serialize.config.entry.ListConfigEntry;
import ru.gravit.launcher.serialize.config.entry.StringConfigEntry;
import ru.gravit.launcher.serialize.signed.SignedBytesHolder;
import ru.gravit.launcher.serialize.signed.SignedObjectHolder;
import ru.gravit.launcher.serialize.stream.EnumSerializer;
import ru.gravit.launcher.serialize.stream.StreamObject;

public class LauncherEngine {
    @LauncherAPI
    public static void addLauncherClassBindings(Map<String, Object> bindings) {
        bindings.put("LauncherClass", Launcher.class);
        bindings.put("LauncherConfigClass", LauncherConfig.class);
        bindings.put("HTTPRequestClass", HTTPRequest.class);

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
        bindings.put("CustomRequestClass", CustomRequest.class);
        bindings.put("PingRequestClass", PingRequest.class);
        bindings.put("AuthRequestClass", AuthRequest.class);
        bindings.put("JoinServerRequestClass", JoinServerRequest.class);
        bindings.put("CheckServerRequestClass", CheckServerRequest.class);
        bindings.put("UpdateRequestClass", UpdateRequest.class);
        bindings.put("LauncherRequestClass", LauncherRequest.class);
        bindings.put("ProfileByUsernameRequestClass", ProfileByUsernameRequest.class);
        bindings.put("ProfileByUUIDRequestClass", ProfileByUUIDRequest.class);
        bindings.put("BatchProfileByUsernameRequestClass", BatchProfileByUsernameRequest.class);

        // Set hasher class bindings
        bindings.put("FileNameMatcherClass", FileNameMatcher.class);
        bindings.put("HashedDirClass", HashedDir.class);
        bindings.put("HashedFileClass", HashedFile.class);
        bindings.put("HashedEntryTypeClass", HashedEntry.Type.class);

        // Set serialization class bindings
        bindings.put("HInputClass", HInput.class);
        bindings.put("HOutputClass", HOutput.class);
        bindings.put("StreamObjectClass", StreamObject.class);
        bindings.put("StreamObjectAdapterClass", StreamObject.Adapter.class);
        bindings.put("SignedBytesHolderClass", SignedBytesHolder.class);
        bindings.put("SignedObjectHolderClass", SignedObjectHolder.class);
        bindings.put("EnumSerializerClass", EnumSerializer.class);

        // Set config serialization class bindings
        bindings.put("ConfigObjectClass", ConfigObject.class);
        bindings.put("ConfigObjectAdapterClass", ConfigObject.Adapter.class);
        bindings.put("BlockConfigEntryClass", BlockConfigEntry.class);
        bindings.put("BooleanConfigEntryClass", BooleanConfigEntry.class);
        bindings.put("IntegerConfigEntryClass", IntegerConfigEntry.class);
        bindings.put("ListConfigEntryClass", ListConfigEntry.class);
        bindings.put("StringConfigEntryClass", StringConfigEntry.class);
        bindings.put("ConfigEntryTypeClass", ConfigEntry.Type.class);
        bindings.put("TextConfigReaderClass", TextConfigReader.class);
        bindings.put("TextConfigWriterClass", TextConfigWriter.class);

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
        bindings.put("FunctionalBridgeClass",FunctionalBridge.class);
        bindings.put("LauncherSettingsClass",LauncherSettings.class);

        // Load JS API if available
        bindings.put("RingProgressIndicatorClass", RingProgressIndicator.class);
        bindings.put("RingProgressIndicatorSkinClass", RingProgressIndicatorSkin.class);
        try {
            Class.forName("javafx.application.Application");
            bindings.put("JSApplicationClass", JSApplication.class);
        } catch (ClassNotFoundException ignored) {
            LogHelper.warning("JavaFX API isn't available");
        }
    }

    public static void main(String... args) throws Throwable {
        JVMHelper.checkStackTrace(LauncherEngine.class);
        JVMHelper.verifySystemProperties(Launcher.class, true);
        EnvHelper.checkDangerousParametrs();
        LogHelper.printVersion("Launcher");
        // Start Launcher
        Instant start = Instant.now();
        try {
            new LauncherEngine().start(args);
        } catch (Exception e) {
            LogHelper.error(e);
            return;
        }
        Instant end = Instant.now();
        LogHelper.debug("Launcher started in %dms", Duration.between(start, end).toMillis());
    }

    // Instance
    private final AtomicBoolean started = new AtomicBoolean(false);

    private final ScriptEngine engine = CommonHelper.newScriptEngine();

    private LauncherEngine() {
        setScriptBindings();
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
        addLauncherClassBindings(bindings);
    }

    @LauncherAPI
    public void start(String... args) throws Throwable {
        Launcher.modulesManager = new ClientModuleManager(this);
        LauncherConfig.getAutogenConfig().initModules(); //INIT
        Launcher.modulesManager.preInitModules();
        Objects.requireNonNull(args, "args");
        if (started.getAndSet(true))
            throw new IllegalStateException("Launcher has been already started");
        Launcher.modulesManager.initModules();
        // Load init.js script
        loadScript(Launcher.API_SCRIPT_FILE);
        loadScript(Launcher.INIT_SCRIPT_FILE);
        loadScript("config.js");
        loadScript("dialog/dialog.js");
        LogHelper.info("Invoking start() function");
        Invocable invoker = (Invocable) engine;
        if (Launcher.isUsingAvanguard()) {
            AvanguardStarter.start(DirBridge.dir);
            AvanguardStarter.loadVared();
            AvanguardStarter.main(false);
        }
        Launcher.modulesManager.postInitModules();
        invoker.invokeFunction("start", (Object) args);
    }
}
