package pro.gravit.launchserver.auth.core.interfaces.provider;

import java.util.List;

public interface AuthSupportRemoteClientAccess {
    String getClientApiUrl();

    List<String> getClientApiFeatures();
}
