package pro.gravit.launcher.debug;

import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launcher.LauncherEngine;
import pro.gravit.launcher.api.AuthService;
import pro.gravit.launcher.events.request.AuthRequestEvent;
import pro.gravit.launcher.events.request.ProfilesRequestEvent;
import pro.gravit.launcher.gui.RuntimeProvider;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launcher.request.Request;
import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launcher.request.update.ProfilesRequest;
import pro.gravit.utils.helper.LogHelper;

import java.util.ArrayList;
import java.util.Arrays;
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
            ClientPermissions permissions = new ClientPermissions();
            if(mainClass == null) {
                throw new NullPointerException("Add `-Dlauncher.runtime.mainclass=YOUR_MAIN_CLASS` to jvmArgs");
            }
            if(accessToken != null) {
                Request.setOAuth(authId, new AuthRequestEvent.OAuthRequestEvent(accessToken, refreshToken, expire));
                Request.RequestRestoreReport report = Request.restore(true, false);
                permissions = report.userInfo.permissions;
                username = report.userInfo.playerProfile.username;
                uuid = report.userInfo.playerProfile.uuid.toString();
                if(report.userInfo.accessToken != null) {
                    minecraftAccessToken = report.userInfo.accessToken;
                }
            } else if(password != null) {
                AuthRequest request = new AuthRequest(login, password, authId, AuthRequest.ConnectTypes.API);
                AuthRequestEvent event = request.request();
                Request.setOAuth(authId, event.oauth);
                if(event.accessToken != null) {
                    minecraftAccessToken = event.accessToken;
                }
                username = event.playerProfile.username;
                uuid = event.playerProfile.uuid.toString();
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
            Class<?> mainClazz = Class.forName(mainClass);
            mainClazz.getMethod("main", String[].class).invoke(null, (Object) newArgs.toArray(new String[0]));
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
