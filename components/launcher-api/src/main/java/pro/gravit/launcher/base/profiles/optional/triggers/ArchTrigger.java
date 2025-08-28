package pro.gravit.launcher.base.profiles.optional.triggers;

import pro.gravit.launcher.base.profiles.optional.OptionalFile;
import pro.gravit.utils.helper.JVMHelper;

public class ArchTrigger extends OptionalTrigger {
    public JVMHelper.ARCH arch;

    @Override
    protected boolean isTriggered(OptionalFile optional, OptionalTriggerContext context) {
        return context.getJavaVersion().arch == arch;
    }
}
