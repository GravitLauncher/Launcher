package ru.gravit.launcher.client;

import ru.gravit.launcher.NewLauncherSettings;
import ru.gravit.launcher.downloader.ListDownloader;
import ru.gravit.launcher.events.request.UpdateRequestEvent;
import ru.gravit.launcher.hasher.HashedDir;
import ru.gravit.launcher.hasher.HashedEntry;
import ru.gravit.launcher.hasher.HashedFile;
import ru.gravit.launcher.managers.SettingsManager;
import ru.gravit.launcher.request.update.UpdateRequest;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class LauncherUpdateController implements UpdateRequest.UpdateController {
    @Override
    public void preUpdate(UpdateRequest request, UpdateRequestEvent e) {

    }

    @Override
    public void preDiff(UpdateRequest request, UpdateRequestEvent e) {

    }

    @Override
    public void postDiff(UpdateRequest request, UpdateRequestEvent e, HashedDir.Diff diff) throws IOException {
        if(e.zip) return;
        if(SettingsManager.settings.featureStore)
        {
            LogHelper.info("Enabled HStore feature. Find");
            AtomicReference<NewLauncherSettings.HashedStoreEntry> lastEn = new AtomicReference<>(null);
            ArrayList<String> removed = new ArrayList<>();
            diff.mismatch.walk(File.separator, (path, name, entry) -> {
                if(entry.getType() == HashedEntry.Type.DIR) {
                    Files.createDirectories(request.getDir().resolve(path));
                    return HashedDir.WalkAction.CONTINUE;
                }
                HashedFile file = (HashedFile) entry;
                //Первый экспериментальный способ - честно обходим все возможные Store
                Path ret = null;
                if(lastEn.get() == null)
                {
                    for(NewLauncherSettings.HashedStoreEntry en : SettingsManager.settings.lastHDirs)
                    {
                        ret = tryFind(en, file);
                        if(ret != null) {
                            lastEn.set(en);
                            break;
                        }
                    }
                }
                else {
                    ret = tryFind(lastEn.get(), file);
                }
                if(ret == null)
                {
                    for(NewLauncherSettings.HashedStoreEntry en : SettingsManager.settings.lastHDirs)
                    {
                        ret = tryFind(en, file);
                        if(ret != null) {
                            lastEn.set(en);
                            break;
                        }
                    }
                }
                if(ret != null)
                {
                    //Еще раз проверим корректность хеша
                    //Возможно эта проверка избыточна
                    //if(file.isSame(ret, true))
                    {
                        Path source = request.getDir().resolve(path);
                        LogHelper.debug("Copy file %s to %s", ret.toAbsolutePath().toString(), source.toAbsolutePath().toString());
                        //Let's go!
                        Files.copy(ret, source);
                        try(InputStream input = IOHelper.newInput(ret))
                        {
                            IOHelper.transfer(input, source);
                        }
                        removed.add(path.replace('\\', '/'));
                    }
                }
                return HashedDir.WalkAction.CONTINUE;
            });
            for(String rem : removed)
            {
                diff.mismatch.removeR(rem);
            }
        }
    }
    public Path tryFind(NewLauncherSettings.HashedStoreEntry en, HashedFile file) throws IOException
    {
        AtomicReference<Path> ret = new AtomicReference<>(null);
        en.hdir.walk(File.separator, (path, name, entry) -> {
            if(entry.getType() == HashedEntry.Type.DIR) return HashedDir.WalkAction.CONTINUE;
            HashedFile tfile = (HashedFile) entry;
            if(tfile.isSame(file))
            {
                LogHelper.dev("[DIR:%s] Found file %s in %s", en.name, name, path);
                Path tdir = Paths.get(en.fullPath).resolve(path);
                try {
                    if(tfile.isSame(tdir, true))
                    {
                        LogHelper.dev("[DIR:%s] Confirmed file %s in %s", en.name, name, path);
                        ret.set(tdir);
                        return HashedDir.WalkAction.STOP;
                    }
                } catch (IOException e)
                {
                    LogHelper.error("Check file error %s %s", e.getClass().getName(), e.getMessage());
                }
            }
            return HashedDir.WalkAction.CONTINUE;
        });
        return ret.get();
    }

    @Override
    public void preDownload(UpdateRequest request, UpdateRequestEvent e, List<ListDownloader.DownloadTask> adds) {

    }

    @Override
    public void postDownload(UpdateRequest request, UpdateRequestEvent e) {

    }

    @Override
    public void postUpdate(UpdateRequest request, UpdateRequestEvent e) {

    }
}
