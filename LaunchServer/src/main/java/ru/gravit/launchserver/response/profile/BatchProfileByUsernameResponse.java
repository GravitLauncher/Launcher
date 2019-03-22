package ru.gravit.launchserver.response.profile;

import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.SerializeLimits;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.response.Response;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.utils.helper.VerifyHelper;

import java.io.IOException;
import java.util.Arrays;

public final class BatchProfileByUsernameResponse extends Response {

    public BatchProfileByUsernameResponse(LaunchServer server, long session, HInput input, HOutput output, String ip, Client clientData) {
        super(server, session, input, output, ip, clientData);
    }

    @Override
    public void reply() throws IOException {
        int length = input.readLength(SerializeLimits.MAX_BATCH_SIZE);
        String[] usernames = new String[length];
        String[] clients = new String[length];
        for (int i = 0; i < usernames.length; i++) {
            usernames[i] = VerifyHelper.verifyUsername(input.readString(64));
            clients[i] = input.readString(SerializeLimits.MAX_CLIENT);
        }
        debug("Usernames: " + Arrays.toString(usernames));

        // Respond with profiles array
        for (int i = 0; i < usernames.length; i++)
            ProfileByUsernameResponse.writeProfile(server, output, usernames[i], clients[i], clientData.auth);
    }
}
