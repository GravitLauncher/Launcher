package ru.gravit.launchserver.auth.handler;

import ru.gravit.launcher.ClientPermissions;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.auth.handler.AuthHandler;
import ru.gravit.utils.helper.CommonHelper;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.net.URL;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RequestAuthHandler extends AuthHandler {
    private String url;
    private transient Pattern pattern;
    private String response;
    private String username;
    private String uuid;
    private String accessToken;
    private String serverID;

    @Override
    public void init() {
        if (url == null) LogHelper.error("[Verify][AuthHandler] url cannot be null");
        if (response == null) LogHelper.error("[Verify][AuthHandler] response cannot be null");
        pattern = Pattern.compile(response);
    }

    @Override
    public void close() {
        // Do nothing
    }
}
