package pro.gravit.launcher.client;

import java.util.Collection;

public interface ClientWrapperModule {
    void wrapperPhase(ProcessBuilder processBuilder, Collection<String> jvmArgs);
}
