package ru.gravit.launcher.managers;

import ru.gravit.launcher.NeedGarbageCollection;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class GarbageManager {
    static class Entry {
        NeedGarbageCollection invoke;
        long timer;

        public Entry(NeedGarbageCollection invoke, long timer) {
            this.invoke = invoke;
            this.timer = timer;
        }
    }

    private static final Timer timer = new Timer("GarbageTimer");

    private static final ArrayList<Entry> NEED_GARBARE_COLLECTION = new ArrayList<>();

    public static void gc() {
        for (Entry gc : NEED_GARBARE_COLLECTION)
            gc.invoke.garbageCollection();
    }

    public static void registerNeedGC(NeedGarbageCollection gc) {
        NEED_GARBARE_COLLECTION.add(new Entry(gc, 0L));
    }

    public static void registerNeedGC(NeedGarbageCollection gc, long time) {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                gc.garbageCollection();
            }
        };
        timer.schedule(task, time);
        NEED_GARBARE_COLLECTION.add(new Entry(gc, time));
    }

    public static void unregisterNeedGC(NeedGarbageCollection gc) {
        NEED_GARBARE_COLLECTION.removeIf(e -> e.invoke == gc);
    }
}
