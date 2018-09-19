package ru.gravit.launchserver.command.basic;

import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.command.Command;
import ru.gravit.utils.HttpDownloader;
import ru.gravit.utils.helper.LogHelper;

import java.net.URL;

public class TestCommand extends Command {
    public TestCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return null;
    }

    @Override
    public String getUsageDescription() {
        return "Test command. Only developer!";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args,1);
        LogHelper.debug("start downloading");
        HttpDownloader.downloadFile(new URL(args[0]),"test.html");
        LogHelper.debug("end downloading");
    }
}
