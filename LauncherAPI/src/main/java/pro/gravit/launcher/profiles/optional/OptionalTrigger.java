package pro.gravit.launcher.profiles.optional;

import pro.gravit.utils.helper.JVMHelper;

public class OptionalTrigger {
    public TriggerType type;
    public boolean need = true;
    public long value;
    public long compareMode = 0;

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

    public enum TriggerType {
        JAVA_VERSION, JAVA_BITS, OS_BITS, OS_TYPE
    }
}
