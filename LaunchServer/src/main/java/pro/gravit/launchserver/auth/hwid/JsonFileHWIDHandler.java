package pro.gravit.launchserver.auth.hwid;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import com.google.gson.reflect.TypeToken;

import pro.gravit.launcher.hwid.HWID;
import pro.gravit.launcher.Launcher;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

public class JsonFileHWIDHandler extends HWIDHandler {
    public class Entry {
        public HWID hwid;
        public String username;
        public boolean isBanned = false;

        public Entry(HWID hwid) {
            this.hwid = hwid;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Entry entry = (Entry) o;
            return Objects.equals(hwid, entry.hwid);
        }

        @Override
        public int hashCode() {
            return Objects.hash(hwid);
        }
    }

    public String filename = "hwids.json";
    public transient LinkedList<Entry> list = new LinkedList<>();
    public String banMessage = "You banned";

    @Override
    public void ban(List<HWID> hwid) {
        for (Entry e : list) {
            for (HWID banHWID : hwid) {
                if (e.hwid.equals(banHWID)) e.isBanned = true;
            }
        }
    }

    @Override
    public void init() {
        Path path = Paths.get(filename);
        Type type = new TypeToken<LinkedList<Entry>>() {
        }.getType();
        try (Reader reader = IOHelper.newReader(path)) {
            list = Launcher.gsonManager.gson.fromJson(reader, type);
        } catch (IOException e) {
            LogHelper.error(e);
        }
    }

    @Override
    public void check0(HWID hwid, String username) throws HWIDException {
        boolean isOne = false;
        for (Entry e : list) {
            if (e.hwid.equals(hwid)) {
                isOne = true;
                if (e.isBanned) throw new HWIDException(banMessage);
            }
        }
        if (!isOne) {
            list.add(new Entry(hwid));
        }
    }

    @Override
    public void close() throws Exception {
        Path path = Paths.get(filename);
        try (Writer writer = IOHelper.newWriter(path)) {
            Launcher.gsonManager.configGson.toJson(list, writer);
        }
    }

    @Override
    public List<HWID> getHwid(String username) {
        LinkedList<HWID> hwids = new LinkedList<>();
        for (Entry e : list) {
            if (e.username.equals(username)) hwids.add(e.hwid);
        }
        return hwids;
    }

    @Override
    public void unban(List<HWID> hwid) {
        for (Entry e : list) {
            for (HWID banHWID : hwid) {
                if (e.hwid.equals(banHWID)) e.isBanned = false;
            }
        }
    }
}
