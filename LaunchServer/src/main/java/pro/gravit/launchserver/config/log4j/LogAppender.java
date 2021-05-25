package pro.gravit.launchserver.config.log4j;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

@Plugin(name = "LogCollect", category = "Core", elementType = "appender", printObject = true)
public class LogAppender extends AbstractAppender {
    private static volatile LogAppender INSTANCE;
    private final Set<Consumer<LogEvent>> set = new HashSet<>();

    public LogAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions, Property[] properties) {
        super(name, filter, layout, ignoreExceptions, properties);
        INSTANCE = this;
    }

    public static LogAppender getInstance() {
        return INSTANCE;
    }

    @PluginFactory
    public static LogAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Filter") final Filter filter,
            @PluginAttribute("otherAttribute") String otherAttribute) {
        if (name == null) {
            LOGGER.error("No name provided for MyCustomAppenderImpl");
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        return new LogAppender(name, filter, layout, true, Property.EMPTY_ARRAY);
    }

    @Override
    public void append(LogEvent event) {
        try {
            for (Consumer<LogEvent> consumer : set) {
                consumer.accept(event);
            }
        } catch (Throwable e) {
            if (!ignoreExceptions()) {
                throw new AppenderLoggingException(e);
            }
        }

    }

    public void addListener(Consumer<LogEvent> consumer) {
        set.add(consumer);
    }

    public void removeListener(Consumer<LogEvent> consumer) {
        set.remove(consumer);
    }
}
