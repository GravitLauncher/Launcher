package ru.gravit.launchserver.manangers.hook;

import ru.gravit.launchserver.socket.Client;
import ru.gravit.launchserver.websocket.json.auth.AuthResponse;
import ru.gravit.launchserver.websocket.json.auth.CheckServerResponse;
import ru.gravit.launchserver.websocket.json.auth.JoinServerResponse;
import ru.gravit.launchserver.websocket.json.auth.SetProfileResponse;
import ru.gravit.utils.BiHookSet;

public class AuthHookManager {
    public BiHookSet<AuthResponse.AuthContext, Client> preHook = new BiHookSet<>();
    public BiHookSet<AuthResponse.AuthContext, Client> postHook = new BiHookSet<>();
    public BiHookSet<CheckServerResponse, Client> checkServerHook = new BiHookSet<>();
    public BiHookSet<JoinServerResponse, Client> joinServerHook = new BiHookSet<>();
    public BiHookSet<SetProfileResponse, Client> setProfileHook = new BiHookSet<>();
}
