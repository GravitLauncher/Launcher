package pro.gravit.launchserver.auth.core.interfaces.user;

import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launcher.profiles.Texture;

public interface UserSupportTextures {
    Texture getSkinTexture();

    Texture getCloakTexture();

    default Texture getSkinTexture(ClientProfile profile) {
        return getSkinTexture();
    }

    default Texture getCloakTexture(ClientProfile profile) {
        return getCloakTexture();
    }
}
