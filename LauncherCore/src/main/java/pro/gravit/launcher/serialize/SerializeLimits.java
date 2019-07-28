package pro.gravit.launcher.serialize;

import pro.gravit.launcher.LauncherAPI;

public class SerializeLimits {
    @LauncherAPI
    public static final int MAX_BATCH_SIZE = 128;
    @LauncherAPI
    public static final byte EXPECTED_BYTE = 0b01010101;
    @LauncherAPI
    public static final int MAX_DIGEST = 512;
}
