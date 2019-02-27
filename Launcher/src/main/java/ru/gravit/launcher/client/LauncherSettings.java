package ru.gravit.launcher.client;

import ru.gravit.launcher.Launcher;
import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.hasher.HashedDir;
import ru.gravit.launcher.profiles.ClientProfile;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.utils.helper.*;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.io.IOException;
import java.nio.file.Path;
import java.security.SignatureException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class LauncherSettings {
    public static int settingsMagic = 0xc0dea;
    @LauncherAPI
    public Path file = DirBridge.dir.resolve("settings.bin");
    @LauncherAPI
    public String login;
    @LauncherAPI
    public byte[] rsaPassword;
    @LauncherAPI
    public int profile;
    @LauncherAPI
    public Path updatesDir;
    @LauncherAPI
    public boolean autoEnter;
    @LauncherAPI
    public boolean fullScreen;
    @LauncherAPI
    public boolean offline;
    @LauncherAPI
    public int ram;

    @LauncherAPI
    public byte[] lastDigest;
    @LauncherAPI
    public List<ClientProfile> lastProfiles = new LinkedList<>();
    @LauncherAPI
    public Map<String, HashedDir> lastHDirs = new HashMap<>(16);

    @LauncherAPI
    public void load() throws SignatureException {
        LogHelper.debug("Loading settings file");
        try {
            try (HInput input = new HInput(IOHelper.newInput(file))) {
                read(input);
            }
        } catch (IOException e) {
            LogHelper.error(e);
            setDefault();
        }
    }

    @LauncherAPI
    public void save() {
        LogHelper.debug("Save settings file");
        try {
            try (HOutput output = new HOutput(IOHelper.newOutput(file))) {
                write(output);
            }
        } catch (IOException e) {
            LogHelper.error(e);
            setDefault();
        }
    }

    @LauncherAPI
    public void read(HInput input) throws IOException, SignatureException {
        int magic = input.readInt();
        if (magic != settingsMagic) {
            setDefault();
            LogHelper.warning("Settings magic mismatch: " + java.lang.Integer.toString(magic, 16));
            return;
            //throw new java.io.IOException();
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
        DirBridge.dirUpdates = updatesDir;
        autoEnter = input.readBoolean();
        fullScreen = input.readBoolean();
        setRAM(input.readLength(JVMHelper.RAM));

        // Offline cache
        lastDigest = input.readBoolean() ? input.readByteArray(0) : null;
        lastProfiles.clear();
        int lastProfilesCount = input.readLength(0);
        for (int i = 0; i < lastProfilesCount; i++) {
            lastProfiles.add(Launcher.gson.fromJson(input.readString(0), ClientProfile.class));
        }
        lastHDirs.clear();
        int lastHDirsCount = input.readLength(0);
        for (int i = 0; i < lastHDirsCount; i++) {
            String name = IOHelper.verifyFileName(input.readString(255));
            HashedDir hdir = new HashedDir(input);
            lastHDirs.put(name,hdir);
        }
    }

    @LauncherAPI
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
        output.writeBoolean(lastDigest != null);
        if (lastDigest != null) {
            output.writeByteArray(lastDigest, 0);
        }
        output.writeLength(lastProfiles.size(), 0);
        for (ClientProfile profile : lastProfiles) {
            output.writeString(Launcher.gson.toJson(profile), 0);
        }
        output.writeLength(lastHDirs.size(), 0);
        for (Map.Entry<String, HashedDir> entry : lastHDirs.entrySet()) {
            output.writeString(entry.getKey(), 0);
            entry.getValue().write(output);
        }
    }

    @LauncherAPI
    public void setRAM(int ram) {
        this.ram = java.lang.Math.min(((ram / 256)) * 256, JVMHelper.RAM);
    }

    @LauncherAPI
    public void setDefault() {
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
        lastDigest = null;
        lastProfiles.clear();
        lastHDirs.clear();
    }

    @LauncherAPI
    public byte[] setPassword(String password) throws BadPaddingException, IllegalBlockSizeException {
        byte[] encrypted = SecurityHelper.newRSAEncryptCipher(Launcher.getConfig().publicKey).doFinal(IOHelper.encode(password));
        return encrypted;
    }
}
