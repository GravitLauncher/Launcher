package pro.gravit.launchserver.auth.hwid;

import pro.gravit.launcher.hwid.HWID;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class MemoryHWIDHandler extends HWIDHandler {
    public static class Entry {
        public final HWID hwid;
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

    public final transient LinkedList<Entry> list = new LinkedList<>();
    public final String banMessage = "You banned";

    @Override
    public void ban(List<HWID> hwid) {
        for (Entry e : list) {
            for (HWID banHWID : hwid) {
                if (e.hwid.equals(banHWID)) {
                    e.isBanned = true;
                    break;
                }
            }
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
    public void close() {

    }

    @Override
    public void init() {

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
                if (e.hwid.equals(banHWID)) {
                    e.isBanned = false;
                    break;
                }
            }
        }
    }
}
