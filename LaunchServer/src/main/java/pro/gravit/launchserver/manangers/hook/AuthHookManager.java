package pro.gravit.launchserver.manangers.hook;

import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;
import pro.gravit.launchserver.socket.response.auth.CheckServerResponse;
import pro.gravit.launchserver.socket.response.auth.JoinServerResponse;
import pro.gravit.launchserver.socket.response.auth.SetProfileResponse;
import pro.gravit.utils.BiHookSet;
import pro.gravit.utils.HookSet;

public class AuthHookManager {
    public BiHookSet<AuthResponse.AuthContext, Client> preHook = new BiHookSet<>();
    public BiHookSet<AuthResponse.AuthContext, Client> postHook = new BiHookSet<>();
    public BiHookSet<CheckServerResponse, Client> checkServerHook = new BiHookSet<>();
    public BiHookSet<JoinServerResponse, Client> joinServerHook = new BiHookSet<>();
    public BiHookSet<SetProfileResponse, Client> setProfileHook = new BiHookSet<>();
    public static class RegContext
    {
        public String login;
        public String password;
        public String ip;
        public boolean trustContext;
        public RegContext(String login, String password, String ip, boolean trustContext) {
            this.login = login;
            this.password = password;
            this.ip = ip;
            this.trustContext = trustContext;
        }
    }
    public HookSet<RegContext> registraion = new HookSet<>();
}
