package ru.gravit.launchserver.response.admin;

import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.SerializeLimits;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.response.Response;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.utils.helper.LogHelper;

import java.io.IOException;

public class ExecCommandResponse extends Response {
    public ExecCommandResponse(LaunchServer server, long session, HInput input, HOutput output, String ip, Client clientData) {
        super(server, session, input, output, ip, clientData);
    }

    @Override
    public void reply() throws Exception {
        Client clientData = server.sessionManager.getClient(session);
        if (!clientData.isAuth || !clientData.permissions.canAdmin || !server.config.enableRcon)
            requestError("Access denied");
        writeNoError(output);
        String cmd = input.readString(SerializeLimits.MAX_COMMAND);
        LogHelper.OutputEnity loutput = new LogHelper.OutputEnity(message -> {
            try {
                output.writeBoolean(true);
                output.writeString(message, SerializeLimits.MAX_COMMAND);
            } catch (IOException e) {
                LogHelper.error(e);
            }
        }, LogHelper.OutputTypes.PLAIN);
        LogHelper.addOutput(loutput);
        try {
            server.commandHandler.eval(cmd, false);
            output.writeBoolean(false);
        } finally {
            LogHelper.removeOutput(loutput);
        }
        writeNoError(output);
    }
}
