package pro.gravit.launcher.client;

import pro.gravit.launcher.NewLauncherSettings;
import pro.gravit.launcher.events.request.UpdateRequestEvent;
import pro.gravit.launcher.hasher.HashedDir;
import pro.gravit.launcher.hasher.HashedEntry;
import pro.gravit.launcher.hasher.HashedFile;
import pro.gravit.launcher.managers.SettingsManager;
import pro.gravit.launcher.request.update.UpdateRequest;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicReference;
@Deprecated
public class LauncherUpdateController {

    public void postDiff(UpdateRequest request, UpdateRequestEvent e, HashedDir.Diff diff) throws IOException {
        if (e.zip && e.fullDownload) return;
        if (SettingsManager.settings.featureStore) {
            LogHelper.info("Enabled HStore feature. Find");
            AtomicReference<NewLauncherSettings.HashedStoreEntry> lastEn = new AtomicReference<>(null);
            //ArrayList<String> removed = new ArrayList<>();
            diff.mismatch.walk(File.separator, (path, name, entry) -> {
                if (entry.getType() == HashedEntry.Type.DIR) {
                    Files.createDirectories(request.getDir().resolve(path));
                    return HashedDir.WalkAction.CONTINUE;
                }
                HashedFile file = (HashedFile) entry;
                //Первый экспериментальный способ - честно обходим все возможные Store
                Path ret = null;
                if (lastEn.get() == null) {
                    for (NewLauncherSettings.HashedStoreEntry en : SettingsManager.settings.lastHDirs) {
                        ret = tryFind(en, file);
                        if (ret != null) {
                            lastEn.set(en);
                            break;
                        }
                    }
                } else {
                    ret = tryFind(lastEn.get(), file);
                }
                if (ret == null) {
                    for (NewLauncherSettings.HashedStoreEntry en : SettingsManager.settings.lastHDirs) {
                        ret = tryFind(en, file);
                        if (ret != null) {
                            lastEn.set(en);
                            break;
                        }
                    }
                }
                if (ret != null) {
                    //Еще раз проверим корректность хеша
                    //Возможно эта проверка избыточна
                    //if(file.isSame(ret, true))
                    {
                        Path source = request.getDir().resolve(path);
                        if (LogHelper.isDebugEnabled()) {
                            LogHelper.debug("Copy file %s to %s", ret.toAbsolutePath().toString(), source.toAbsolutePath().toString());
                        }
                        //Let's go!
                        Files.deleteIfExists(source);
                        Files.copy(ret, source);
                        try (InputStream input = IOHelper.newInput(ret)) {
                            IOHelper.transfer(input, source);
                        }
                        entry.flag = true;
                        //removed.add(path.replace('\\', '/'));
                    }
                }
                return HashedDir.WalkAction.CONTINUE;
            });
        }
    }

    public Path tryFind(NewLauncherSettings.HashedStoreEntry en, HashedFile file) throws IOException {
        AtomicReference<Path> ret = new AtomicReference<>(null);
        en.hdir.walk(File.separator, (path, name, entry) -> {
            if (entry.getType() == HashedEntry.Type.DIR) return HashedDir.WalkAction.CONTINUE;
            HashedFile tfile = (HashedFile) entry;
            if (tfile.isSame(file)) {
                if (LogHelper.isDevEnabled()) {
                    LogHelper.dev("[DIR:%s] Found file %s in %s", en.name, name, path);
                }
                Path tdir = Paths.get(en.fullPath).resolve(path);
                try {
                    if (tfile.isSame(tdir, true)) {
                        if (LogHelper.isDevEnabled()) {
                            LogHelper.dev("[DIR:%s] Confirmed file %s in %s", en.name, name, path);
                        }
                        ret.set(tdir);
                        return HashedDir.WalkAction.STOP;
                    }
                } catch (IOException e) {
                    LogHelper.error("Check file error %s %s", e.getClass().getName(), e.getMessage());
                }
            }
            return HashedDir.WalkAction.CONTINUE;
        });
        return ret.get();
    }
}
