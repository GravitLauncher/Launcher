package pro.gravit.launcher.runtime.console;

import pro.gravit.launcher.base.request.management.GetConnectUUIDRequest;
import pro.gravit.utils.command.Command;
import pro.gravit.utils.helper.LogHelper;

public class GetConnectUUIDCommand extends Command {
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
        LogHelper.info("Your connectUUID: %s | shardId %d", response.connectUUID.toString(), response.shardId);
    }
}
