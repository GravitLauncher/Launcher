package pro.gravit.launchserver.socket;

import io.netty.util.concurrent.DefaultThreadFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NettyThreadFactory extends DefaultThreadFactory {
    private transient final Logger logger = LogManager.getLogger();

    public NettyThreadFactory(String poolName) {
        super(poolName);
    }

    @Override
    protected Thread newThread(Runnable r, String name) {
        Thread thread = super.newThread(r, name);
        thread.setUncaughtExceptionHandler((th, e) -> {
            if (e.getMessage().contains("Connection reset by peer")) {
                return;
            }
            logger.error("Netty exception", e);
        });
        return thread;
    }
}
