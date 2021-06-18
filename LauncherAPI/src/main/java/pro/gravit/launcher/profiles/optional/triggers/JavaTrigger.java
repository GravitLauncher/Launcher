package pro.gravit.launcher.profiles.optional.triggers;

import pro.gravit.launcher.profiles.optional.OptionalFile;
import pro.gravit.utils.helper.JavaHelper;

public class JavaTrigger extends OptionalTrigger {
    public int minVersion;
    public int maxVersion;
    public boolean requireJavaFX;

    public JavaTrigger(int minVersion, int maxVersion, boolean requireJavaFX) {
        this.minVersion = minVersion;
        this.maxVersion = maxVersion;
        this.requireJavaFX = requireJavaFX;
    }

    public JavaTrigger(int minVersion, int maxVersion) {
        this.minVersion = minVersion;
        this.maxVersion = maxVersion;
        this.requireJavaFX = false;
    }

    public JavaTrigger() {
        this.minVersion = 8;
        this.maxVersion = 999;
        this.requireJavaFX = false;
    }

    @Override
    public boolean isTriggered(OptionalFile optional, OptionalTriggerContext context) {
        JavaHelper.JavaVersion version = context.getJavaVersion();
        if (version.version < minVersion) return false;
        if (version.version > maxVersion) return false;
        if (requireJavaFX && !version.enabledJavaFX) return false;
        return true;
    }
}
