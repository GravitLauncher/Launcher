package ru.gravit.launcher;

import ru.gravit.launcher.client.ClientLauncher;
import ru.gravit.utils.helper.EnvHelper;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.JVMHelper;
import ru.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ClientLauncherWrapper {
    @LauncherAPI
    public static void main(String[] arguments) throws IOException, InterruptedException {
        LogHelper.printVersion("Launcher");
        JVMHelper.checkStackTrace(ClientLauncherWrapper.class);
        JVMHelper.verifySystemProperties(Launcher.class, true);
        EnvHelper.checkDangerousParametrs();
        LogHelper.debug("Restart Launcher");
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.inheritIO();
        Path javaBin = IOHelper.resolveJavaBin(Paths.get(System.getProperty("java.home")));
        List<String> args = new LinkedList<>();
        args.add(javaBin.toString());
        String pathLauncher = IOHelper.getCodeSource(ClientLauncher.class).toString();
        Collections.addAll(args, "-javaagent:".concat(pathLauncher));
        Collections.addAll(args, LauncherEngine.class.getName());
        EnvHelper.addEnv(processBuilder);
        processBuilder.command(args);
        Process process = processBuilder.start();
        if(!LogHelper.isDebugEnabled()) {
            Thread.sleep(3000);
            if (!process.isAlive()) {
                LogHelper.error("Process error code: %d", process.exitValue());
            } else {
                LogHelper.debug("Process started success");
            }
        }
        else
        {
            process.waitFor();
        }
    }
}
