package pro.gravit.utils.helper;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import pro.gravit.utils.Version;

public final class FormatHelper {
    private FormatHelper() { }

    public static boolean isAnsiEnabled() {
        return !Boolean.getBoolean("launcher.noJAnsi") && AnsiSupport.AVAILABLE;
    }

    public static String ansiFormatVersion(String product) {
        var version = Version.getVersion();
        return new Ansi().bold()
                .fgBright(Ansi.Color.MAGENTA).a("GravitLauncher ")
                .fgBright(Ansi.Color.BLUE).a("(fork sashok724's Launcher) ")
                .fgBright(Ansi.Color.CYAN).a(product)
                .fgBright(Ansi.Color.WHITE).a(" v")
                .fgBright(Ansi.Color.BLUE).a(version)
                .fgBright(Ansi.Color.WHITE).a(" (build #")
                .fgBright(Ansi.Color.RED).a(version.build)
                .fgBright(Ansi.Color.WHITE).a(')')
                .reset().toString();
    }

    public static String ansiFormatLicense(String product) {
        return new Ansi().bold()
                .fgBright(Ansi.Color.MAGENTA).a("License for ")
                .fgBright(Ansi.Color.CYAN).a(product)
                .fgBright(Ansi.Color.WHITE).a(" GPLv3. SourceCode: ")
                .fgBright(Ansi.Color.YELLOW).a("https://github.com/GravitLauncher/Launcher")
                .reset().toString();
    }

    public static String formatVersion(String product) {
        return String.format("GravitLauncher (fork sashok724's Launcher) %s v%s", product, Version.getVersion());
    }

    public static String formatLicense(String product) {
        return String.format("License for %s GPLv3. SourceCode: https://github.com/GravitLauncher/Launcher", product);
    }

    private static final class AnsiSupport {
        private static final boolean AVAILABLE = initialize();

        private static boolean initialize() {
            try {
                AnsiConsole.systemInstall();
                return true;
            } catch (LinkageError | RuntimeException e) {
                return false;
            }
        }
    }
}
