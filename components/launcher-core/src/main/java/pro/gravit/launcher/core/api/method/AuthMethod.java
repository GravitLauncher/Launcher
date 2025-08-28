package pro.gravit.launcher.core.api.method;

import java.util.List;
import java.util.Set;

public interface AuthMethod {
    List<AuthMethodDetails> getDetails();
    String getName();
    String getDisplayName();
    boolean isVisible();
    Set<String> getFeatures();
}
