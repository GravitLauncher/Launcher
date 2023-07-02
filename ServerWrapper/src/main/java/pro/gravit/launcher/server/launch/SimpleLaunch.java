package pro.gravit.launcher.server.launch;

import pro.gravit.launcher.server.ServerWrapper;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class SimpleLaunch implements Launch {

    @Override
    @SuppressWarnings("ConfusingArgumentToVarargsMethod")
    public void run(String mainClass, ServerWrapper.Config config, String[] args) throws Throwable {
        MethodHandles.lookup()
                .findStatic(Class.forName(mainClass), "main", MethodType.methodType(void.class, String[].class))
                .invoke(args);
    }
}
