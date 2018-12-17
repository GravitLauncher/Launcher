package ru.gravit.launcher;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import ru.gravit.launcher.client.ClientLauncher;
import ru.gravit.utils.helper.EnvHelper;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.JVMHelper;
import ru.gravit.utils.helper.LogHelper;

public class ClientLauncherWrapper {
    public static void main(String[] arguments) throws IOException, InterruptedException {
        LogHelper.printVersion("Launcher");
        LogHelper.printLicense("Launcher");
        LogHelper.info("Restart Launcher witch JavaAgent...");
        LogHelper.info("If need debug output use -Dlauncher.debug=true");
        JVMHelper.checkStackTrace(ClientLauncherWrapper.class);
        JVMHelper.verifySystemProperties(Launcher.class, true);
        EnvHelper.checkDangerousParams();
        LogHelper.debug("Restart Launcher");
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.inheritIO();
        Path javaBin = IOHelper.resolveJavaBin(Paths.get(System.getProperty("java.home")));
        List<String> args = new LinkedList<>();
        args.add(javaBin.toString());
        String pathLauncher = IOHelper.getCodeSource(ClientLauncher.class).toString();
        args.add(JVMHelper.jvmProperty(LogHelper.DEBUG_PROPERTY, Boolean.toString(LogHelper.isDebugEnabled())));
        Collections.addAll(args, "-noverify", "-javaagent:".concat(pathLauncher), "-cp", pathLauncher, LauncherEngine.class.getName());
        EnvHelper.addEnv(processBuilder);
        LogHelper.debug("Commandline: " + args);
        processBuilder.command(args);
        Process process = processBuilder.start();
        if (!LogHelper.isDebugEnabled()) {
            Thread.sleep(3000);
            if (!process.isAlive()) {
                int errorcode = process.exitValue();
                if(errorcode != 0)
                    LogHelper.error("Process exit witch error code: %d", errorcode);
                else
                    LogHelper.info("Process exit witch code 0");
            } else {
                LogHelper.debug("Process started success");
            }
        } else {
            process.waitFor();
        }
    }
}
