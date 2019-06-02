package pro.gravit.launchserver.command.service;

import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.NeedGarbageCollection;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.launchserver.components.Component;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.Reader;
import java.nio.file.Paths;

public class ComponentCommand extends Command {
    public ComponentCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[action] [component name] [more args]";
    }

    @Override
    public String getUsageDescription() {
        return "component manager";
    }

    public void printHelp() {
        LogHelper.info("Print help for component:");
        LogHelper.subInfo("component unload [componentName]");
        LogHelper.subInfo("component load [componentName] [filename]");
        LogHelper.subInfo("component gc [componentName]");
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 1);
        String componentName = null;
        if (args.length > 1) componentName = args[1];
        switch (args[0]) {
            case "unload": {
                if (componentName == null) throw new IllegalArgumentException("Must set componentName");
                Component component = server.config.components.get(componentName);
                if (component == null) {
                    LogHelper.error("Component %s not found", componentName);
                    return;
                }
                if (component instanceof AutoCloseable) {
                    ((AutoCloseable) component).close();
                } else {
                    LogHelper.error("Component %s unload not supported", componentName);
                    return;
                }
                break;
            }
            case "gc": {
                if (componentName == null) throw new IllegalArgumentException("Must set componentName");
                Component component = server.config.components.get(componentName);
                if (component == null) {
                    LogHelper.error("Component %s not found", componentName);
                    return;
                }
                if (component instanceof NeedGarbageCollection) {
                    ((NeedGarbageCollection) component).garbageCollection();
                } else {
                    LogHelper.error("Component %s gc not supported", componentName);
                    return;
                }
                break;
            }
            case "load": {
                if (componentName == null) throw new IllegalArgumentException("Must set componentName");
                if (args.length <= 2) throw new IllegalArgumentException("Must set file");
                String fileName = args[2];
                try (Reader reader = IOHelper.newReader(Paths.get(fileName))) {
                    Component component = Launcher.gsonManager.configGson.fromJson(reader, Component.class);
                    component.preInit(server);
                    component.init(server);
                    component.postInit(server);
                    LogHelper.info("Component %s(%s) loaded", componentName, component.getClass().getName());
                }
            }
            case "help": {
                printHelp();
            }
            default: {
                printHelp();
            }
        }
    }
}
