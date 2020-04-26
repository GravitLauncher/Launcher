package pro.gravit.launcher.api;

import pro.gravit.launcher.LauncherEngine;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.utils.helper.LogHelper;

public class SystemService {
    private SystemService() {
        throw new UnsupportedOperationException();
    }

    public static void exit(int code) {
        LauncherEngine.exitLauncher(code);
    }

    public static void setSecurityManager(SecurityManager s) {
        LogHelper.debug("Try set security manager %s", s == null ? "null" : s.getClass().getName());
        if (AuthService.profile == null || AuthService.profile.securityManagerConfig == ClientProfile.SecurityManagerConfig.NONE)
            return;
        if (AuthService.profile.securityManagerConfig == ClientProfile.SecurityManagerConfig.CLIENT) {
            System.setSecurityManager(s);
        }
        //TODO NEXT
    }
}
