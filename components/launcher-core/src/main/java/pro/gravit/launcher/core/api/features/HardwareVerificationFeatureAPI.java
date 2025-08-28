package pro.gravit.launcher.core.api.features;

import pro.gravit.utils.helper.JVMHelper;

import java.security.PublicKey;
import java.util.concurrent.CompletableFuture;

public interface HardwareVerificationFeatureAPI extends FeatureAPI {
    CompletableFuture<SecurityLevelInfo> getSecurityInfo();
    CompletableFuture<SecurityLevelVerification> privateKeyVerification(PublicKey publicKey, byte[] signature);
    CompletableFuture<Void> sendHardwareInfo(HardwareStatisticData statisticData, HardwareIdentifyData identifyData);
    
    
    interface SecurityLevelInfo {
        boolean isRequired();
        byte[] getSignData();
    }
    interface SecurityLevelVerification {
        HardwareCollectLevel getHardwareCollectLevel();
        
        enum HardwareCollectLevel {
            NONE, ONLY_STATISTIC, ALL
        }
    }

    record HardwareStatisticData(Arch arch, Os os, long totalPhysicalMemory,
                              int logicalProcessors, int physicalProcessors,
                                 long processorMaxFreq, boolean battery,
                                 String graphicCard) {

    }

    record HardwareIdentifyData(String baseboardSerialNumber, String persistentStorageId,
                                byte[] edid) {

    }

    enum Arch {
        X86("x86"), X86_64("x86-64"), ARM64("arm64"), ARM32("arm32");

        public final String name;

        Arch(String name) {
            this.name = name;
        }
    }

    enum Os {
        WINDOWS("windows"), LINUX("linux"), MACOS("macos");

        public final String name;

        Os(String name) {
            this.name = name;
        }
    }
}
