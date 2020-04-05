package pro.gravit.launcher.managers;

import pro.gravit.launcher.NewLauncherSettings;
import pro.gravit.launcher.client.DirBridge;
import pro.gravit.launcher.config.JsonConfigurable;
import pro.gravit.launcher.hasher.HashedDir;
import pro.gravit.launcher.serialize.HInput;
import pro.gravit.launcher.serialize.HOutput;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class SettingsManager extends JsonConfigurable<NewLauncherSettings> {
    public static NewLauncherSettings settings;


    public SettingsManager() {
        super(NewLauncherSettings.class, DirBridge.dir.resolve("settings.json"));
    }

    @Override
    public NewLauncherSettings getConfig() {
        return settings;
    }

    @Override
    public void setConfig(NewLauncherSettings config) {
        settings = config;
        if (settings.consoleUnlockKey != null && !ConsoleManager.isConsoleUnlock) {
            if (ConsoleManager.checkUnlockKey(settings.consoleUnlockKey)) {
                ConsoleManager.unlock();
                LogHelper.info("Console auto unlocked");
            }
        }
    }

    @Override
    public NewLauncherSettings getDefaultConfig() {
        return new NewLauncherSettings();
    }

    public void loadHDirStore(Path storePath) throws IOException {
        Files.createDirectories(storePath);
        IOHelper.walk(storePath, new StoreFileVisitor(), false);
    }

    public void saveHDirStore(Path storeProjectPath) throws IOException {
        Files.createDirectories(storeProjectPath);
        for (NewLauncherSettings.HashedStoreEntry e : settings.lastHDirs) {
            if (!e.needSave) continue;
            Path file = storeProjectPath.resolve(e.name.concat(".bin"));
            if (!Files.exists(file)) Files.createFile(file);
            try (HOutput output = new HOutput(IOHelper.newOutput(file))) {
                output.writeString(e.name, 128);
                output.writeString(e.fullPath, 1024);
                e.hdir.write(output);
            }
        }
    }

    public void loadHDirStore() throws IOException {
        loadHDirStore(DirBridge.dirStore);
    }

    public void saveHDirStore() throws IOException {
        saveHDirStore(DirBridge.dirProjectStore);
    }

    public static class StoreFileVisitor extends SimpleFileVisitor<Path> {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException {
            try (HInput input = new HInput(IOHelper.newInput(file))) {
                String dirName = input.readString(128);
                String fullPath = input.readString(1024);
                HashedDir dir = new HashedDir(input);
                settings.lastHDirs.add(new NewLauncherSettings.HashedStoreEntry(dir, dirName, fullPath));
            } catch (IOException e) {
                LogHelper.error("Skip file %s exception: %s", file.toAbsolutePath().toString(), e.getMessage());
            }
            return super.visitFile(file, attrs);
        }

    }
}
