package pro.gravit.launcher.profiles.optional;

import pro.gravit.launcher.profiles.optional.triggers.JavaTrigger;
import pro.gravit.launcher.profiles.optional.triggers.OSTrigger;
import pro.gravit.utils.helper.JVMHelper;

@Deprecated
public class OptionalTrigger {
    public TriggerType type;
    public boolean need = true;
    public long value;
    public long compareMode = 0;

    public OptionalTrigger() {
    }

    public OptionalTrigger(TriggerType type, long value) {
        this.type = type;
        this.value = value;
    }

    public OptionalTrigger(TriggerType type, boolean need, long value, long compareMode) {
        this.type = type;
        this.need = need;
        this.value = value;
        this.compareMode = compareMode;
    }

    public boolean isTriggered() {
        long test;
        switch (type) {

            case JAVA_VERSION:
                test = JVMHelper.JVM_VERSION;
                break;
            case JAVA_BITS:
                test = JVMHelper.JVM_BITS;
                break;
            case OS_BITS:
                test = JVMHelper.OS_BITS;
                break;
            case OS_TYPE:
                switch (JVMHelper.OS_TYPE) {

                    case MUSTDIE:
                        test = 0;
                        break;
                    case LINUX:
                        test = 1;
                        break;
                    case MACOSX:
                        test = 2;
                        break;
                    default:
                        return false;
                }
                break;
            default:
                return false;
        }
        if (compareMode == 0) return test == value;
        else if (compareMode < 0) return test < value;
        else return test > value;
    }

    public pro.gravit.launcher.profiles.optional.triggers.OptionalTrigger toTrigger() {
        switch (type) {
            case JAVA_VERSION: {
                JavaTrigger trigger = new JavaTrigger((int) value, (int) value);
                trigger.required = need;
                if (compareMode > 0) {
                    trigger.maxVersion = 999;
                } else if (compareMode < 0) {
                    trigger.minVersion = 0;
                }
                return trigger;
            }
            case JAVA_BITS:
            case OS_BITS:
                return null;
            case OS_TYPE: {
                JVMHelper.OS os;
                if (value == 0) os = JVMHelper.OS.MUSTDIE;
                else if (value == 1) os = JVMHelper.OS.LINUX;
                else if (value == 2) os = JVMHelper.OS.MUSTDIE;
                else throw new IllegalArgumentException(String.format("Os version %d unknown", value));
                OSTrigger trigger = new OSTrigger(os);
                trigger.required = need;
                if (compareMode != 0) trigger.inverted = true;
                return trigger;
            }
        }
        return null;
    }

    public enum TriggerType {
        JAVA_VERSION, JAVA_BITS, OS_BITS, OS_TYPE
    }
}
