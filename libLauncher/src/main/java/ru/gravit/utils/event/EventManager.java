package ru.gravit.utils.event;

import ru.gravit.utils.helper.LogHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class EventManager {
    public static final int QUEUE_MAX_SIZE = 2048;
    public static final int INITIAL_HANDLERS_SIZE = 16;
    public class Entry
    {
        public Entry(EventHandler<EventInterface> func, UUID[] events) {
            this.func = func;
            this.events = events;
        }

        EventHandler<EventInterface> func;
        UUID[] events;
    }
    public class QueueEntry
    {
        public QueueEntry(EventInterface event, UUID key) {
            this.event = event;
            this.key = key;
        }

        EventInterface event;
        UUID key;
    }
    public ArrayList<Entry> handlers = new ArrayList<>(INITIAL_HANDLERS_SIZE);
    public BlockingQueue<QueueEntry> queue = new LinkedBlockingQueue<>(QUEUE_MAX_SIZE); //Максимальный размер очереди
    public int registerHandler(EventHandler<EventInterface> func, UUID[] events)
    {
        Arrays.sort(events);
        handlers.add(new Entry(func,events));
        return handlers.size();
    }
    public void sendEvent(UUID key, EventInterface event, boolean blocking)
    {
        if(blocking) process(key,event);
        else queue.add(new QueueEntry(event,key));
    }
    public void process(UUID key, EventInterface event)
    {
        for(Entry e : handlers)
        {
            if(Arrays.binarySearch(e.events,key) >= 0) e.func.run(event);
        }
    }
    public class EventExecutor implements Runnable {
        public boolean enable = true;
        @Override
        public void run() {
            while(enable && !Thread.interrupted())
            {
                try {
                    QueueEntry e = queue.take();
                    process(e.key,e.event);
                } catch (InterruptedException e) {
                    LogHelper.error(e);
                }
                Thread.yield();
            }
        }
    }
}
