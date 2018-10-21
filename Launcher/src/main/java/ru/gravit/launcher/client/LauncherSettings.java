package ru.gravit.launcher.client;

import ru.gravit.launcher.Launcher;
import ru.gravit.launcher.LauncherConfig;
import ru.gravit.launcher.hasher.HashedDir;
import ru.gravit.launcher.profiles.ClientProfile;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.signed.SignedObjectHolder;
import ru.gravit.utils.event.EventManager;
import ru.gravit.utils.helper.*;

import java.io.IOException;
import java.nio.file.Path;
import java.security.SignatureException;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class LauncherSettings {
    public static int settingsMagic;
    Path file = DirBridge.dir.resolve("settings.bin");
    String login;
    byte[] rsaPassword;
    int profile;
    Path updatesDir;
    boolean autoEnter;
    boolean fullScreen;
    int ram;

    byte[] lastSign;
    LinkedList<SignedObjectHolder<ClientProfile>> lastProfiles;
    HashMap<String,SignedObjectHolder<HashedDir>> lastHDirs;
    public void load()
    {
        LogHelper.debug("Loading settings file");
        try {
            try(HInput input = new HInput(IOHelper.newInput(file)))
            {

            }
        } catch(IOException e) {
            LogHelper.error(e);
            setDefault();
        }
    }
    public void read(HInput input) throws IOException, SignatureException
    {
        int magic = input.readInt();
        if (magic != settingsMagic) {
            throw new java.io.IOException("Settings magic mismatch: " + java.lang.Integer.toString(magic, 16));
        }

        // Launcher settings
        boolean debug = input.readBoolean();
        if (!LogHelper.isDebugEnabled() && debug) {
            LogHelper.setDebugEnabled(true);
        }

        // Auth settings
        login = input.readBoolean() ? input.readString(255) : null;
        rsaPassword = input.readBoolean() ? input.readByteArray(IOHelper.BUFFER_SIZE) : null;
        profile = input.readLength(0);

        // Client settings
        updatesDir = IOHelper.toPath(input.readString(0));
        autoEnter = input.readBoolean();
        fullScreen = input.readBoolean();
        setRAM(input.readLength(JVMHelper.RAM));

        // Offline cache
        RSAPublicKey publicKey = Launcher.getConfig().publicKey;
        lastSign = input.readBoolean() ? input.readByteArray(-SecurityHelper.RSA_KEY_LENGTH) : null;
        lastProfiles.clear();
        int lastProfilesCount = input.readLength(0);
        for (int i = 0; i < lastProfilesCount; i++) {
            lastProfiles.add(new SignedObjectHolder<>(input, publicKey, ClientProfile.RO_ADAPTER));
        }
        lastHDirs.clear();
        int lastHDirsCount = input.readLength(0);
        for (int i = 0; i < lastHDirsCount; i++) {
            String name = IOHelper.verifyFileName(input.readString(255));
            VerifyHelper.putIfAbsent(lastHDirs, name, new SignedObjectHolder<>(input, publicKey, HashedDir::new),
            java.lang.String.format("Duplicate offline hashed dir: '%s'", name));
        }
        //cliParams.applySettings();
    }
    public void write(HOutput output) throws IOException {
        output.writeInt(settingsMagic);

        // Launcher settings
        output.writeBoolean(LogHelper.isDebugEnabled());

        // Auth settings
        output.writeBoolean(login != null);
        if (login != null) {
            output.writeString(login, 255);
        }
        output.writeBoolean(rsaPassword != null);
        if (rsaPassword != null) {
            output.writeByteArray(rsaPassword, IOHelper.BUFFER_SIZE);
        }
        output.writeLength(profile, 0);

        // Client settings
        output.writeString(IOHelper.toString(updatesDir), 0);
        output.writeBoolean(autoEnter);
        output.writeBoolean(fullScreen);
        output.writeLength(ram, JVMHelper.RAM);

        // Offline cache
        output.writeBoolean(lastSign != null);
        if (lastSign != null) {
            output.writeByteArray(lastSign, -SecurityHelper.RSA_KEY_LENGTH);
        }
        output.writeLength(lastProfiles.size(), 0);
        for(SignedObjectHolder<ClientProfile> profile : lastProfiles) {
        profile.write(output);
    }
        output.writeLength(lastHDirs.size(), 0);
        for(Map.Entry<String,SignedObjectHolder<HashedDir>> entry : lastHDirs.entrySet()) {
        output.writeString(entry.getKey(), 0);
        entry.getValue().write(output);
    }
    }
    public void setRAM(int ram)
    {
        ram = java.lang.Math.min(((ram / 256)) * 256, JVMHelper.RAM);
    }
    public void setDefault()
    {
        // Auth settings
        login = null;
        rsaPassword = null;
        profile = 0;

        // Client settings
        //==DEFAULT==
        updatesDir = DirBridge.defaultUpdatesDir;
        autoEnter = false;
        fullScreen = false;
        setRAM(1024);
        //==========

        // Offline cache
        lastSign = null;
        lastProfiles.clear();
        lastHDirs.clear();

        // Apply CLI params
        //cliParams.applySettings();
    }
}
