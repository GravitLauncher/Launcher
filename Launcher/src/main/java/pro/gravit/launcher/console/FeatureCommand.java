package pro.gravit.launcher.console;

import pro.gravit.launcher.managers.SettingsManager;
import pro.gravit.utils.command.Command;
import pro.gravit.utils.helper.LogHelper;

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
