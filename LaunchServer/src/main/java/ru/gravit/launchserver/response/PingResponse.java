package ru.gravit.launchserver.response;

import java.io.IOException;

import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.SerializeLimits;
import ru.gravit.launchserver.LaunchServer;

public final class PingResponse extends Response {
    public PingResponse(LaunchServer server, long id, HInput input, HOutput output, String ip) {
        super(server, id, input, output, ip);
    }

    @Override
    public void reply() throws IOException {
        output.writeUnsignedByte(SerializeLimits.EXPECTED_BYTE);
    }
}
