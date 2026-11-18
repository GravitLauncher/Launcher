package com.gravitlauncher.launchserver.cabinetstarter.embedded;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.slf4j.LoggerFactory;
import pro.gravit.launchserver.config.log4j.LogAppender;

final class CabinetLogging {
    private CabinetLogging() {
    }

    static LoggerContext initialize() {
        var builder = ConfigurationBuilderFactory.newConfigurationBuilder();
        builder.setConfigurationName("SimpleCabinet");
        builder.setShutdownHook("disable");
        builder.add(builder.newRootLogger(Level.ALL));
        var config = builder.build(false);
        var context = Configurator.initialize(CabinetLogging.class.getClassLoader(), config);
        if (context == null) throw new IllegalStateException("Unable to initialize LaunchServer logging");

        var appender = new LogAppender("SimpleCabinet", null, null, true, Property.EMPTY_ARRAY);
        appender.addListener(CabinetLogging::forward);
        appender.start();
        config.addAppender(appender);
        config.getRootLogger().addAppender(appender, null, null);
        context.updateLoggers();
        return context;
    }

    private static void forward(LogEvent event) {
        int severity = event.getLevel().intLevel();
        var level = severity <= Level.ERROR.intLevel() ? org.slf4j.event.Level.ERROR
                : severity <= Level.WARN.intLevel() ? org.slf4j.event.Level.WARN
                : severity <= Level.INFO.intLevel() ? org.slf4j.event.Level.INFO
                : severity <= Level.DEBUG.intLevel() ? org.slf4j.event.Level.DEBUG : org.slf4j.event.Level.TRACE;
        LoggerFactory.getLogger(event.getLoggerName()).atLevel(level)
                .setCause(event.getThrown()).log(event.getMessage()::getFormattedMessage);
    }
}
