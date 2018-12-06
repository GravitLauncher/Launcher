package ru.gravit.launcher;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import ru.gravit.utils.helper.LogHelper;

public class RequestWorker implements Runnable {
    public RequestWorker()
    {
        queue = new LinkedBlockingQueue<>(64);
    }
    public BlockingQueue<Runnable> queue;
    @Override
    public void run() {
        LogHelper.debug("FX Task Thread start");
        while (!Thread.interrupted())
        {
            try {
                Runnable task;
                task = queue.take();
                task.run();
            } catch (InterruptedException e) {
                LogHelper.error(e);
                return;
            }
        }
        LogHelper.debug("FX Task Thread done");
    }
}
