package ru.gravit.launcher.server;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Path;
import java.nio.file.Paths;

import ru.gravit.launcher.Launcher;
import ru.gravit.launcher.LauncherConfig;
import ru.gravit.launcher.client.ClientLauncher;
import ru.gravit.launcher.serialize.config.ConfigObject;
import ru.gravit.launcher.serialize.config.TextConfigReader;
import ru.gravit.launcher.serialize.config.TextConfigWriter;
import ru.gravit.launcher.serialize.config.entry.BlockConfigEntry;
import ru.gravit.launcher.serialize.config.entry.StringConfigEntry;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.launcher.profiles.ClientProfile;
import ru.gravit.launcher.request.update.ProfilesRequest;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.signed.SignedObjectHolder;

public class ServerWrapper {
    public static ModulesManager modulesManager;
    public static Path configFile;
    public static Config config;
    public static void main(String[] args) throws Throwable {
        ServerWrapper wrapper = new ServerWrapper();
        modulesManager = new ModulesManager(wrapper);
        modulesManager.autoload(Paths.get("modules"));
        Launcher.modulesManager = modulesManager;
        LauncherConfig cfg = new LauncherConfig(new HInput(IOHelper.newInput(IOHelper.getResourceURL(Launcher.CONFIG_FILE))));
        configFile = IOHelper.WORKING_DIR.resolve("ServerWrapper.cfg");
        modulesManager.preInitModules();
        generateConfigIfNotExists();
        try (BufferedReader reader = IOHelper.newReader(configFile)) {
            config = new Config(TextConfigReader.read(reader, true));
        }
        ProfilesRequest.Result result = new ProfilesRequest(cfg).request();
        for (SignedObjectHolder<ClientProfile> p : result.profiles) {
            LogHelper.debug("Get profile: %s", p.object.getTitle());
            if (p.object.getTitle().equals(ClientLauncher.profile.getTitle())) {
                wrapper.profile = p.object;
                ClientLauncher.setProfile(p.object);
                LogHelper.debug("Found profile: %s", ClientLauncher.profile.getTitle());
                break;
            }
        }
        modulesManager.initModules();
        String classname = args[0];
        Class<?> mainClass = Class.forName(classname);
        MethodHandle mainMethod = MethodHandles.publicLookup().findStatic(mainClass, "main", MethodType.methodType(void.class, String[].class));
        String[] real_args = new String[args.length - 1];
        System.arraycopy(args, 1, real_args, 0, args.length - 1);
        modulesManager.postInitModules();
        mainMethod.invoke(real_args);
    }
    private static void generateConfigIfNotExists() throws IOException {
        if (IOHelper.isFile(configFile))
            return;

        // Create new config
        LogHelper.info("Creating LaunchServer config");
        Config newConfig;
        try (BufferedReader reader = IOHelper.newReader(IOHelper.getResourceURL("ru/gravit/launcher/server/config.cfg"))) {
            newConfig = new Config(TextConfigReader.read(reader, false));
        }

        LogHelper.warning("Title is not set. Please show ServerWrapper.cfg");

        // Write LaunchServer config
        LogHelper.info("Writing LaunchServer config file");
        try (BufferedWriter writer = IOHelper.newWriter(configFile)) {
            TextConfigWriter.write(newConfig.block, writer, true);
        }
    }
    public static final class Config extends ConfigObject {
        public String title;
        protected Config(BlockConfigEntry block) {
            super(block);
            title = block.getEntryValue("title",StringConfigEntry.class);
        }
    }

    public ClientProfile profile;
}
