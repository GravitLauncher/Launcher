package pro.gravit.launchserver.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pro.gravit.launcher.NeedGarbageCollection;

public abstract class AbstractLimiter<T> extends Component implements NeedGarbageCollection {
    public int rateLimit;
    public int rateLimitMillis;
    public List<T> exclude = new ArrayList<>();

    @Override
    public void garbageCollection() {
        long time = System.currentTimeMillis();
        map.entrySet().removeIf((e) -> e.getValue().time + rateLimitMillis < time);
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
    protected transient Map<T, LimitEntry> map = new HashMap<>();
    public boolean check(T address)
    {
        if(exclude.contains(address)) return true;
        LimitEntry entry = map.get(address);
        if(entry == null)
        {
            map.put(address, new LimitEntry());
            return true;
        }
        else
        {
            long time = System.currentTimeMillis();
            if(entry.trys < rateLimit)
            {
                entry.trys++;
                entry.time = time;
                return true;
            }
            if(entry.time + rateLimitMillis < time)
            {
                entry.trys = 1;
                entry.time = time;
                return true;
            }
            return false;
        }
    }
}
