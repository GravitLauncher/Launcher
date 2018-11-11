package ru.gravit.launcher.client;

import javafx.concurrent.Task;
import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.request.Request;
import ru.gravit.utils.helper.LogHelper;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class RequestWorker implements Runnable {
    public RequestWorker()
    {
        queue = new LinkedBlockingQueue<>(64);
    }
    public BlockingQueue<Task> queue;
    @Override
    public void run() {
        while (!Thread.interrupted())
        {
            Task task = queue.poll();
            if (task != null) {
                task.run();
            }
        }
    }
}
