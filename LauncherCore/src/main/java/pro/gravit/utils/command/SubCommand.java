package pro.gravit.utils.command;

public abstract class SubCommand extends Command {
    private String defaultArgs;
    private String defaultUsage;

    public SubCommand() {
    }

    public SubCommand(String defaultArgs, String defaultUsage) {
        this.defaultArgs = defaultArgs;
        this.defaultUsage = defaultUsage;
    }

    @Override
    public String getArgsDescription() {
        return defaultArgs;
    }

    @Override
    public String getUsageDescription() {
        return defaultUsage;
    }
}
