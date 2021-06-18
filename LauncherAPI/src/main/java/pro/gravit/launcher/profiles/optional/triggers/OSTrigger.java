package pro.gravit.launcher.profiles.optional.triggers;

import pro.gravit.launcher.profiles.optional.OptionalFile;
import pro.gravit.utils.helper.JVMHelper;

public class OSTrigger extends OptionalTrigger {
    public JVMHelper.OS os;

    public OSTrigger(JVMHelper.OS os) {
        this.os = os;
    }

    @Override
    public boolean isTriggered(OptionalFile optional, OptionalTriggerContext context) {
        return JVMHelper.OS_TYPE == os;
    }
}
