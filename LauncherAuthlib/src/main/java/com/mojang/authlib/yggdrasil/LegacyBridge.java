package com.mojang.authlib.yggdrasil;

import pro.gravit.launcher.request.auth.CheckServerRequest;
import pro.gravit.launcher.request.auth.JoinServerRequest;
import pro.gravit.utils.helper.CommonHelper;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

// Used by 1.6.4 and below versions

public class LegacyBridge {
    private LegacyBridge() {
    }

    public static boolean checkServer(String username, String serverID) throws Exception {
        LogHelper.debug("LegacyBridge.checkServer, Username: '%s', Server ID: %s", username, serverID);
        return new CheckServerRequest(username, serverID).request() != null;
    }

    public static String getCloakURL(String username) {
        LogHelper.debug("LegacyBridge.getCloakURL: '%s'", username);
        return CommonHelper.replace(System.getProperty("launcher.legacy.cloaksURL",
                "http://skins.minecraft.net/MinecraftCloaks/%username%.png"), "username", IOHelper.urlEncode(username));
    }

    public static String getSkinURL(String username) {
        LogHelper.debug("LegacyBridge.getSkinURL: '%s'", username);
        return CommonHelper.replace(System.getProperty("launcher.legacy.skinsURL",
                "http://skins.minecraft.net/MinecraftSkins/%username%.png"), "username", IOHelper.urlEncode(username));
    }

    public static String joinServer(String username, String accessToken, String serverID) {

        // Join server
        LogHelper.debug("LegacyBridge.joinServer, Username: '%s', Access token: %s, Server ID: %s", username, accessToken, serverID);
        try {
            return new JoinServerRequest(username, accessToken, serverID).request().allow ? "OK" : "Bad Login (Clientside)";
        } catch (Exception e) {
            return e.toString();
        }
    }
}
