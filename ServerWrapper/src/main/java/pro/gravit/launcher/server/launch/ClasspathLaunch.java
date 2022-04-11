package pro.gravit.launcher.server.launch;

import pro.gravit.launcher.server.ServerWrapper;
import pro.gravit.utils.PublicURLClassLoader;
import pro.gravit.utils.helper.IOHelper;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.nio.file.Paths;

public class ClasspathLaunch implements Launch {
    @Override
    @SuppressWarnings("ConfusingArgumentToVarargsMethod")
    public void run(ServerWrapper.Config config, String[] args) throws Throwable {
        URL[] urls = config.classpath.stream().map(Paths::get).map(IOHelper::toURL).toArray(URL[]::new);
        ClassLoader ucl = new PublicURLClassLoader(urls);
        Class<?> mainClass = Class.forName(config.mainclass, true, ucl);
        MethodHandle mainMethod = MethodHandles.lookup().findStatic(mainClass, "main", MethodType.methodType(void.class, String[].class));
        mainMethod.invoke(args);
    }
}
