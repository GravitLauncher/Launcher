package ru.gravit.launcher.console;

import ru.gravit.launcher.managers.SettingsManager;
import ru.gravit.utils.command.Command;
import ru.gravit.utils.helper.LogHelper;

public class FeatureCommand extends Command {
    @Override
    public String getArgsDescription() {
        return "[feature] [true/false]";
    }

    @Override
    public String getUsageDescription() {
        return "Enable or disable feature";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 2);
        boolean enabled = Boolean.valueOf(args[1]);
        switch (args[0]) {
            case "store": {
                SettingsManager.settings.featureStore = enabled;
                break;
            }
            default: {
                LogHelper.info("Features: [store]");
                return;
            }
        }
        LogHelper.info("Feature %s %s", args[0], enabled ? "enabled" : "disabled");
    }
}
