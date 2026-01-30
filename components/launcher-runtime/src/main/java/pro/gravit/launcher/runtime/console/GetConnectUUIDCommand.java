package pro.gravit.launcher.runtime.console;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.gravit.launcher.base.request.management.GetConnectUUIDRequest;
import pro.gravit.utils.command.Command;
import pro.gravit.utils.helper.LogHelper;

public class GetConnectUUIDCommand extends Command {

    private static final Logger logger =
            LoggerFactory.getLogger(GetConnectUUIDCommand.class);

    @Override
    public String getArgsDescription() {
        return null;
    }

    @Override
    public String getUsageDescription() {
        return "Get your connectUUID";
    }

    @Override
    public void invoke(String... args) throws Exception {
        var response = new GetConnectUUIDRequest().request();
        logger.info("Your connectUUID: {} | shardId {}", response.connectUUID.toString(), response.shardId);
    }
}