package ru.gravit.launcher.request;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.stream.EnumSerializer;

import java.io.IOException;

public enum RequestType implements EnumSerializer.Itf {
    PING(0), // Ping request
    LEGACYLAUNCHER(1), UPDATE(2), UPDATE_LIST(3), // Update requests
    AUTH(4), JOIN_SERVER(5), CHECK_SERVER(6), // Auth requests
    PROFILE_BY_USERNAME(7), PROFILE_BY_UUID(8), BATCH_PROFILE_BY_USERNAME(9), // Profile requests
    PROFILES(10), SERVERAUTH(11), SETPROFILE(12), LAUNCHER(13), CHANGESERVER(14), EXECCOMMAND(15),
    CUSTOM(255); // Custom requests
    private static final EnumSerializer<RequestType> SERIALIZER = new EnumSerializer<>(RequestType.class);

    @LauncherAPI
    public static RequestType read(HInput input) throws IOException {
        return SERIALIZER.read(input);
    }

    private final int n;

    RequestType(int n) {
        this.n = n;
    }

    @Override
    public int getNumber() {
        return n;
    }
}
