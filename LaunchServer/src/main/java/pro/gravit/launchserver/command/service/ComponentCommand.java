package pro.gravit.launchserver.command.service;

import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.NeedGarbageCollection;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.launchserver.components.Component;
import pro.gravit.utils.command.SubCommand;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.Reader;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Paths;

public class ComponentCommand extends Command {
    public ComponentCommand(LaunchServer server) {
        super(server);
        childCommands.put("unload", new UnloadCommand());
        childCommands.put("load", new LoadCommand());
    }

    private class UnloadCommand extends SubCommand {
        public UnloadCommand() {
            super("[componentName]", "Unload component");
        }

        @Override
        public void invoke(String... args) throws Exception {
            verifyArgs(args, 1);
            String componentName = args[0];
            if (componentName == null) throw new IllegalArgumentException("Must set componentName");
            Component component = server.config.components.get(componentName);
            if (component == null) {
                LogHelper.error("Component %s not found", componentName);
                return;
            }
            if (component instanceof AutoCloseable) {
                ((AutoCloseable) component).close();
            }
            server.config.components.remove(componentName);
            LogHelper.info("Component %s unloaded. Use 'config launchserver save' to save changes");
        }
    }
    private class LoadCommand extends SubCommand {
        public LoadCommand() {
            super("[componentName] [componentType] (json file)", "Load component");
        }

        @Override
        public void invoke(String... args) throws Exception {
            verifyArgs(args, 2);
            String componentName = args[0];
            Class<? extends Component> componentClass = Component.providers.getClass(args[1]);
            if(componentClass == null) {
                LogHelper.error("Component type %s not registered", componentName);
                return;
            }
            try {
                Component component;
                if(args.length > 2) {
                    try (Reader reader = IOHelper.newReader(Paths.get(args[2]))) {
                        component = Launcher.gsonManager.configGson.fromJson(reader, componentClass);
                    }
                } else {
                    component = (Component) MethodHandles.publicLookup().findConstructor(componentClass, MethodType.methodType(void.class)).invoke();
                }
                component.setComponentName(componentName);
                server.config.components.put(componentName, component);
                component.init(server);
                LogHelper.info("Component %s ready. Use 'config launchserver save' to save changes");
            } catch (Throwable throwable) {
                LogHelper.error(throwable);
            }
        }
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
        invokeSubcommands(args);
    }
}
