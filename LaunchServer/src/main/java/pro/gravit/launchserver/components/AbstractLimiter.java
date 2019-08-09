package pro.gravit.launchserver.components;

import pro.gravit.launcher.NeedGarbageCollection;

import java.util.HashMap;
import java.util.Map;

public class AbstractLimiter<T> implements NeedGarbageCollection {
    public final int maxTrys;
    public final int banMillis;

    public AbstractLimiter(int maxTrys, int banMillis) {
        this.maxTrys = maxTrys;
        this.banMillis = banMillis;
    }

    @Override
    public void garbageCollection() {
        long time = System.currentTimeMillis();
        map.entrySet().removeIf((e) -> e.getValue().time + banMillis < time);
    }

    class LimitEntry
    {
        long time;
        int trys;

        public LimitEntry(long time, int trys) {
            this.time = time;
            this.trys = trys;
        }

        public LimitEntry() {
            time = System.currentTimeMillis();
            trys = 0;
        }
    }
    protected Map<T, LimitEntry> map = new HashMap<>();
    public boolean check(T address)
    {
        LimitEntry entry = map.get(address);
        if(entry == null)
        {
            map.put(address, new LimitEntry());
            return true;
        }
        else
        {
            long time = System.currentTimeMillis();
            if(entry.trys < maxTrys)
            {
                entry.trys++;
                entry.time = time;
                return true;
            }
            if(entry.time + banMillis < time)
            {
                entry.trys = 1;
                entry.time = time;
                return true;
            }
            return false;
        }
    }
}
