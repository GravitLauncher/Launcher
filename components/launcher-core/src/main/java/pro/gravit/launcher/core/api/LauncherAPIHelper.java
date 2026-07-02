package pro.gravit.launcher.core.api;

import pro.gravit.launcher.core.api.features.HardwareVerificationFeatureAPI;
import pro.gravit.utils.helper.JVMHelper;

public class LauncherAPIHelper {

    public static HardwareVerificationFeatureAPI.Os toHardwareFeatureOs(JVMHelper.OS os) {
        return switch (os) {
            case MUSTDIE -> HardwareVerificationFeatureAPI.Os.WINDOWS;
            case LINUX -> HardwareVerificationFeatureAPI.Os.LINUX;
            case MACOSX -> HardwareVerificationFeatureAPI.Os.MACOS;
        };
    }

    public static HardwareVerificationFeatureAPI.Arch toHardwareFeatureArch(JVMHelper.ARCH arch) {
        return switch (arch) {
            case X86 -> HardwareVerificationFeatureAPI.Arch.X86;
            case X86_64 -> HardwareVerificationFeatureAPI.Arch.X86_64;
            case ARM64 -> HardwareVerificationFeatureAPI.Arch.ARM64;
            case ARM32 -> HardwareVerificationFeatureAPI.Arch.ARM32;
        };
    }
}
