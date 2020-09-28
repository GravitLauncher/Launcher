package pro.gravit.launchserver.socket;

import io.netty.util.concurrent.DefaultThreadFactory;
import pro.gravit.utils.helper.LogHelper;

public class NettyThreadFactory extends DefaultThreadFactory {
    public NettyThreadFactory(String poolName) {
        super(poolName);
    }

    @Override
    protected Thread newThread(Runnable r, String name) {
        Thread thread = super.newThread(r, name);
        thread.setUncaughtExceptionHandler((th, e) -> {
            if (LogHelper.isDebugEnabled())
                LogHelper.error(e);
        });
        return thread;
    }
}
