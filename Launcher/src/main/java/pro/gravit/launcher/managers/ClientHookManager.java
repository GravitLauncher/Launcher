package pro.gravit.launcher.managers;

import pro.gravit.launcher.client.ClientLauncherContext;
import pro.gravit.launcher.gui.RuntimeProvider;
import pro.gravit.launcher.serialize.HInput;
import pro.gravit.launcher.serialize.HOutput;
import pro.gravit.utils.BiHookSet;
import pro.gravit.utils.HookSet;

public class ClientHookManager {
    public static HookSet<RuntimeProvider> initGuiHook = new HookSet<>();
    public static HookSet<HInput> paramsInputHook = new HookSet<>();
    public static HookSet<HOutput> paramsOutputHook = new HookSet<>();

    public static HookSet<ClientLauncherContext> clientLaunchHook = new HookSet<>();
    public static HookSet<ClientLauncherContext> clientLaunchFinallyHook = new HookSet<>();

    public static BiHookSet<ClientLauncherContext, ProcessBuilder> preStartHook = new BiHookSet<>();
    public static BiHookSet<ClientLauncherContext, ProcessBuilder> postStartHook = new BiHookSet<>();
}
