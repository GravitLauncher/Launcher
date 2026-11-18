package com.gravitlauncher.launchserver.cabinetstarter;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import pro.gravit.utils.launch.ClassLoaderControl;
import pro.gravit.utils.launch.LaunchOptions;
import pro.gravit.utils.launch.ModuleLaunch;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class CabinetStarterRunner implements CommandLineRunner, DisposableBean {
    private static final Set<String> CABINET_ADMIN_COMMANDS = Set.of(
            "--create-admin-user", "--admin-change-password", "--create-admin-token");

    private final ConfigurableApplicationContext context;
    private final Environment environment;
    private ModuleLaunch launch;
    private ClassLoader loader;
    private AutoCloseable server;

    public CabinetStarterRunner(ConfigurableApplicationContext context) {
        this.context = context;
        this.environment = context.getEnvironment();
    }

    @Override
    public synchronized void run(String... args) throws Exception {
        if (Arrays.stream(args).anyMatch(CABINET_ADMIN_COMMANDS::contains)) {
            return;
        }

        Path moduleJar = Path.of(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        Path home = path("cabinetstarter.home", moduleJar.resolveSibling("../launchserver"));
        Path workDir = path("cabinetstarter.work-dir", Path.of("."));
        Path modules = path("cabinetstarter.modules-dir",
                Path.of(environment.getProperty("launchserver.dir.modules", home.resolve("modules").toString())));

        String backendUrl = environment.getProperty("cabinetstarter.backend-url");
        if (backendUrl == null || backendUrl.isBlank()) {
            int port = environment.getRequiredProperty("local.server.port", Integer.class);
            String scheme = environment.getProperty("server.ssl.enabled", Boolean.class, false) ? "https" : "http";
            backendUrl = scheme + "://localhost:" + port
                    + environment.getProperty("server.servlet.context-path", "")
                    + environment.getProperty("spring.mvc.servlet.path", "");
        }
        backendUrl = normalizeUrl(backendUrl, "cabinetstarter.backend-url");
        String publicUrl = environment.getProperty("cabinetstarter.api-url", "");
        String apiUrl = publicUrl.isBlank() ? backendUrl : normalizeUrl(publicUrl, "cabinetstarter.api-url");
        String apiKey = readApiKey();
        String projectName = environment.getProperty("cabinetstarter.project-name",
                environment.getProperty("PROJECTNAME", "MineCraft"));

        Path libraries = home.resolve("lib");
        if (!Files.isRegularFile(libraries.resolve("cabinetstarter-embedded.jar"))) {
            throw new IllegalStateException("CabinetStarter runtime is missing from " + libraries);
        }
        List<Path> classpath;
        try (var files = Files.list(libraries)) {
            classpath = files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".jar")).sorted().toList();
        }

        ClassLoader cabinetLoader = getClass().getClassLoader();
        LoggingSystem logging = LoggingSystem.get(cabinetLoader);
        Consumer<Boolean> setTrace = enabled ->
                logging.setLogLevel("pro.gravit", enabled ? LogLevel.TRACE : LogLevel.DEBUG);
        var runtime = new ModuleLaunch(cabinetLoader);
        Thread thread = Thread.currentThread();
        ClassLoader previous = thread.getContextClassLoader();
        try {
            ClassLoaderControl control = runtime.init(classpath, null, new LaunchOptions());
            control.clearLauncherPackages();
            control.addLauncherPackage("pro.gravit.utils.launch.");
            control.addLauncherPackage("org.slf4j.");
            thread.setContextClassLoader(control.getClassLoader());
            Class<?> entry = control.getClass("com.gravitlauncher.launchserver.cabinetstarter.embedded.EmbeddedLaunchServer");
            server = (AutoCloseable) entry.getMethod("start", Path.class, Path.class, Path.class,
                    String.class, String.class, String.class, String.class, Consumer.class, ClassLoaderControl.class, Runnable.class)
                    .invoke(null, home, workDir, modules, apiUrl, backendUrl, apiKey, projectName, setTrace, control,
                            (Runnable) this::stopCabinet);
            loader = control.getClassLoader();
            launch = runtime;
        } catch (Exception | Error failure) {
            Throwable cause = failure instanceof InvocationTargetException invocation ? invocation.getCause() : failure;
            try { runtime.close(); } catch (Exception cleanup) { cause.addSuppressed(cleanup); }
            if (cause instanceof Exception exception) throw exception;
            if (cause instanceof Error error) throw error;
            throw new IllegalStateException(cause);
        } finally {
            thread.setContextClassLoader(previous);
        }
    }

    private String readApiKey() throws IOException {
        String apiKey = environment.getProperty("cabinetstarter.api-key", "");
        if (!apiKey.isBlank()) return apiKey;

        String file = environment.getProperty("cabinetstarter.api-key-file", "");
        if (!file.isBlank()) {
            String token = Files.readString(Path.of(file)).strip();
            if (token.isEmpty()) throw new IllegalArgumentException("cabinetstarter.api-key-file is empty");
            return token;
        }
        return environment.getProperty("API_KEY", environment.getProperty("launchserver.apiKey", ""));
    }

    private void stopCabinet() {
        Thread shutdown = Thread.ofPlatform().name("CabinetShutdown").daemon(false).unstarted(context::close);
        shutdown.setContextClassLoader(context.getClassLoader());
        shutdown.start();
    }

    private Path path(String property, Path fallback) {
        return Path.of(environment.getProperty(property, fallback.toString())).toAbsolutePath().normalize();
    }

    private static String normalizeUrl(String value, String property) {
        URI uri = URI.create(value);
        if (uri.getHost() == null || uri.getRawUserInfo() != null || uri.getRawQuery() != null
                || uri.getRawFragment() != null || !("http".equalsIgnoreCase(uri.getScheme())
                || "https".equalsIgnoreCase(uri.getScheme()))) {
            throw new IllegalArgumentException(property + " must be an absolute HTTP(S) URL");
        }
        return value.replaceAll("/+$", "");
    }

    @Override
    public synchronized void destroy() throws Exception {
        if (loader == null) return;
        Thread thread = Thread.currentThread();
        ClassLoader previous = thread.getContextClassLoader();
        try (var runtime = launch) {
            thread.setContextClassLoader(loader);
            server.close();
        } finally {
            thread.setContextClassLoader(previous);
        }
    }
}
