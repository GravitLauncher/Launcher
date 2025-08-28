package pro.gravit.launchserver.command.basic;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.JVMHelper;

import java.lang.management.RuntimeMXBean;

public final class VersionCommand extends Command {
    private transient final Logger logger = LogManager.getLogger();

    public VersionCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return null;
    }

    @Override
    public String getUsageDescription() {
        return "Print LaunchServer version";
    }

    @Override
    public void invoke(String... args) {
        logger.info("LaunchServer version: {}.{}.{} (build #{})", Version.MAJOR, Version.MINOR, Version.PATCH, Version.BUILD);
        RuntimeMXBean mxBean = JVMHelper.RUNTIME_MXBEAN;
        logger.info("Java {}({})", JVMHelper.getVersion(), mxBean.getVmVersion());
        logger.info("Java Home: {}", System.getProperty("java.home", "UNKNOWN"));
    }
}
