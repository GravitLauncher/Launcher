package pro.gravit.launcher.runtime.debug;

import pro.gravit.launcher.base.ClientPermissions;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.api.AuthService;
import pro.gravit.launcher.base.api.ClientService;
import pro.gravit.launcher.runtime.LauncherEngine;
import pro.gravit.launcher.runtime.gui.RuntimeProvider;
import pro.gravit.launcher.base.events.request.AuthRequestEvent;
import pro.gravit.launcher.base.events.request.ProfilesRequestEvent;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launcher.base.request.Request;
import pro.gravit.launcher.base.request.auth.AuthRequest;
import pro.gravit.launcher.base.request.update.ProfilesRequest;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.launch.*;

import java.io.File;
import java.io.Reader;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ClientRuntimeProvider implements RuntimeProvider {
    @Override
    public void run(String[] args) {
        ArrayList<String> newArgs = new ArrayList<>(Arrays.asList(args));
        try {
            String username = System.getProperty("launcher.runtime.username", null);
            String uuid = System.getProperty("launcher.runtime.uuid", null);
            String login = System.getProperty("launcher.runtime.login", username);
            String password = System.getProperty("launcher.runtime.password", "Player");
            String authId = System.getProperty("launcher.runtime.auth.authid", "std");
            String accessToken = System.getProperty("launcher.runtime.auth.accesstoken", null);
            String refreshToken = System.getProperty("launcher.runtime.auth.refreshtoken", null);
            String minecraftAccessToken = System.getProperty("launcher.runtime.auth.minecraftaccesstoken", "DEBUG");
            long expire = Long.parseLong(System.getProperty("launcher.runtime.auth.expire", "0"));
            String profileUUID = System.getProperty("launcher.runtime.profileuuid", null);
            String mainClass = System.getProperty("launcher.runtime.mainclass", null);
            String mainModule = System.getProperty("launcher.runtime.mainmodule", null);
            String launchMode = System.getProperty("launcher.runtime.launch", "basic");
            String compatClasses = System.getProperty("launcher.runtime.launch.compat", null);
            String nativesDir = System.getProperty("launcher.runtime.launch.natives", "natives");
            String launcherOptionsPath = System.getProperty("launcher.runtime.launch.options", null);
            boolean enableHacks = Boolean.getBoolean("launcher.runtime.launch.enablehacks");
            ClientPermissions permissions = new ClientPermissions();
            if(mainClass == null) {
                throw new NullPointerException("Add `-Dlauncher.runtime.mainclass=YOUR_MAIN_CLASS` to jvmArgs");
            }
            if(uuid == null) {
                if(accessToken != null) {
                    Request.setOAuth(authId, new AuthRequestEvent.OAuthRequestEvent(accessToken, refreshToken, expire));
                    Request.RequestRestoreReport report = Request.restore(true, false, true);
                    permissions = report.userInfo.permissions;
                    username = report.userInfo.playerProfile.username;
                    uuid = report.userInfo.playerProfile.uuid.toString();
                    if(report.userInfo.accessToken != null) {
                        minecraftAccessToken = report.userInfo.accessToken;
                    }
                } else {
                    AuthRequest request = new AuthRequest(login, password, authId, AuthRequest.ConnectTypes.API);
                    AuthRequestEvent event = request.request();
                    Request.setOAuth(authId, event.oauth);
                    if(event.accessToken != null) {
                        minecraftAccessToken = event.accessToken;
                    }
                    username = event.playerProfile.username;
                    uuid = event.playerProfile.uuid.toString();
                }
            }
            if(profileUUID != null) {
                UUID profileUuid = UUID.fromString(profileUUID);
                ProfilesRequest profiles = new ProfilesRequest();
                ProfilesRequestEvent event = profiles.request();
                for(ClientProfile profile : event.profiles) {
                    if(profile.getUUID().equals(profileUuid)) {
                        AuthService.profile = profile;
                    }
                }
            }
            if(username == null) {
                username = "Player";
            }
            if(uuid == null) {
                uuid = "a7899336-e61c-4e51-b480-0c815b18aed8";
            }
            replaceOrCreateArgument(newArgs, "--username", username);
            replaceOrCreateArgument(newArgs, "--uuid", uuid);
            replaceOrCreateArgument(newArgs, "--accessToken", minecraftAccessToken);
            AuthService.uuid = UUID.fromString(uuid);
            AuthService.username = username;
            AuthService.permissions = permissions;
            Launch launch;
            switch (launchMode) {
                case "basic": {
                    launch = new BasicLaunch();
                    break;
                }
                case "legacy": {
                    launch = new LegacyLaunch();
                    break;
                }
                case "module": {
                    launch = new ModuleLaunch();
                    break;
                }
                default: {
                    throw new UnsupportedOperationException(String.format("Unknown launch mode: '%s'", launchMode));
                }
            }
            List<Path> classpath = new ArrayList<>();
            try {
                for(var c : System.getProperty("java.class.path").split(File.pathSeparator)) {
                    classpath.add(Paths.get(c));
                }
            } catch (Throwable e) {
                LogHelper.error(e);
            }
            LaunchOptions options;
            if(launcherOptionsPath != null) {
                try(Reader reader = IOHelper.newReader(Paths.get(launcherOptionsPath))) {
                    options = Launcher.gsonManager.gson.fromJson(reader, LaunchOptions.class);
                }
            } else {
                options = new LaunchOptions();
            }
            options.enableHacks = enableHacks;
            ClassLoaderControl classLoaderControl = launch.init(classpath, nativesDir, options);
            ClientService.classLoaderControl = classLoaderControl;
            if(compatClasses != null) {
                String[] compatClassesList = compatClasses.split(",");
                for (String e : compatClassesList) {
                    Class<?> clazz = classLoaderControl.getClass(e);
                    MethodHandle runMethod = MethodHandles.lookup().findStatic(clazz, "run", MethodType.methodType(void.class, ClassLoaderControl.class));
                    runMethod.invoke(classLoaderControl);
                }
            }
            launch.launch(mainClass, mainModule, Arrays.asList(args));
        } catch (Throwable e) {
            LogHelper.error(e);
            LauncherEngine.exitLauncher(-15);
        }
    }

    public void replaceOrCreateArgument(ArrayList<String> args, String name, String value) {
        int index = args.indexOf(name);
        if(index < 0) {
            args.add(name);
            if(value != null) {
                args.add(value);
            }
            return;
        }
        if(value != null) {
            int valueIndex = index+1;
            args.set(valueIndex, value);
        }
    }

    @Override
    public void preLoad() {

    }

    @Override
    public void init(boolean clientInstance) {

    }
}
