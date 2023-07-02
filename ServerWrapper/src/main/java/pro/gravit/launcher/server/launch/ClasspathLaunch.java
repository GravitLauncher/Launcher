package pro.gravit.launcher.server.launch;

import pro.gravit.launcher.server.ServerWrapper;
import pro.gravit.utils.PublicURLClassLoader;
import pro.gravit.utils.helper.IOHelper;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.nio.file.Paths;

public class ClasspathLaunch implements Launch {

    @Override
    @SuppressWarnings("ConfusingArgumentToVarargsMethod")
    public void run(String mainClass, ServerWrapper.Config config, String[] args) throws Throwable {
        URL[] urls = config.classpath.stream()
                .map(Paths::get)
                .map(IOHelper::toURL)
                .toArray(URL[]::new);

        MethodHandles.lookup()
                .findStatic(Class.forName(mainClass, true, new PublicURLClassLoader(urls)), "main", MethodType.methodType(void.class, String[].class))
                .invoke(args);
    }
}
