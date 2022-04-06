package pro.gravit.launcher.server.launch;

import pro.gravit.launcher.server.ServerWrapper;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class SimpleLaunch implements Launch {
    @Override
    @SuppressWarnings("ConfusingArgumentToVarargsMethod")
    public void run(ServerWrapper.Config config, String[] args) throws Throwable {
        Class<?> mainClass = Class.forName(config.mainclass);
        MethodHandle mainMethod = MethodHandles.lookup().findStatic(mainClass, "main", MethodType.methodType(void.class, String[].class));
        mainMethod.invoke(args);
    }
}
