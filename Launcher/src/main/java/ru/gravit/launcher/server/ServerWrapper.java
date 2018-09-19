package ru.gravit.launcher.server;


import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Paths;

import ru.gravit.launcher.Launcher;
import ru.gravit.launcher.LauncherConfig;
import ru.gravit.launcher.client.ClientLauncher;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.launcher.profiles.ClientProfile;
import ru.gravit.launcher.request.update.ProfilesRequest;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.signed.SignedObjectHolder;

public class ServerWrapper {
    public static ModulesManager modulesManager;
    public static void main(String[] args) throws Throwable {
        ServerWrapper wrapper = new ServerWrapper();
        modulesManager = new ModulesManager(wrapper);
        modulesManager.autoload(Paths.get("modules"));
        Launcher.modulesManager = modulesManager;
        LauncherConfig cfg = new LauncherConfig(new HInput(IOHelper.newInput(IOHelper.getResourceURL(Launcher.CONFIG_FILE))));
        modulesManager.preInitModules();
        ProfilesRequest.Result result = new ProfilesRequest(cfg).request();
        for(SignedObjectHolder<ClientProfile> p : result.profiles)
        {
            LogHelper.debug("Get profile: %s",p.object.getTitle());
            if(p.object.getTitle().equals(ClientLauncher.title)) {
                wrapper.profile = p.object;
                LogHelper.debug("Found profile: %s",ClientLauncher.title);
                break;
            }
        }
        modulesManager.initModules();
        String classname = args[0];
        Class<?> mainClass = Class.forName(classname);
        MethodHandle mainMethod = MethodHandles.publicLookup().findStatic(mainClass, "main", MethodType.methodType(void.class, String[].class));
        String[] real_args = new String[args.length - 1];
        System.arraycopy(args,1,real_args,0,args.length - 1);
        modulesManager.postInitModules();
        mainMethod.invoke(real_args);
    }
    public ClientProfile profile;
}
