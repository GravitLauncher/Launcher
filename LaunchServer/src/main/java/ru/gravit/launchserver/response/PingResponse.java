package ru.gravit.launchserver.response;

import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.SerializeLimits;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.socket.Client;

import java.io.IOException;

public final class PingResponse extends Response {
    public PingResponse(LaunchServer server, long id, HInput input, HOutput output, String ip, Client clientData) {
        super(server, id, input, output, ip, clientData);
    }

    @Override
    public void reply() throws IOException {
        output.writeUnsignedByte(SerializeLimits.EXPECTED_BYTE);
    }
}
