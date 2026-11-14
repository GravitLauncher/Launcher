package pro.gravit.launcher.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import pro.gravit.launcher.base.LauncherConfig;
import pro.gravit.launcher.base.profiles.ClientProfileBuilder;
import pro.gravit.launcher.base.profiles.PlayerProfile;
import pro.gravit.launcher.base.request.RequestFeatureHttpAPIImpl;
import pro.gravit.launcher.base.request.RequestOfflineModeAPIImpl;
import pro.gravit.launcher.core.api.LauncherAPIHolder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ClientAPIInitializationTest {
    @AfterEach
    void reset() {
        LauncherAPIHolder.reset();
    }

    private static LauncherConfig config() {
        return new LauncherConfig("http://127.0.0.1:1/api", null, null, Map.of(), "test");
    }

    private static ClientParams params() {
        var params = new ClientParams();
        params.profile = new ClientProfileBuilder().createClientProfile();
        params.playerProfile = PlayerProfile.newOfflineProfile("Player");
        params.remoteAccessToken = "Player";
        return params;
    }

    @Test
    void propagatesOfflineFlagDespiteOnlineBuildConfiguration() throws Exception {
        var params = params();
        params.offlineMode = true;
        params.remoteAccessToken = null;
        LauncherAPIInitializer.initializeClient(null, config(), params);
        assertInstanceOf(RequestOfflineModeAPIImpl.class, LauncherAPIHolder.auth());
        assertEquals("Player", LauncherAPIHolder.auth().getCurrentUser().join().getUsername());
        assertEquals(params.playerProfile.uuid, LauncherAPIHolder.auth().getCurrentUser().join().getUUID());
    }

    @Test
    void recognizesOfflineAuthSelectionWithoutFlag() throws Exception {
        var params = params();
        params.authId = "offline";
        LauncherAPIInitializer.initializeClient(null, config(), params);
        assertInstanceOf(RequestOfflineModeAPIImpl.class, LauncherAPIHolder.auth());
    }

    @Test
    void restoresOnlineApiWithoutFetchingUserAndAcceptsAbsentAuthId() throws Exception {
        LauncherAPIInitializer.initializeClient(null, config(), params());
        assertInstanceOf(RequestFeatureHttpAPIImpl.class, LauncherAPIHolder.auth());
    }

    @Test
    void missingOnlineTokenFailsAndClosesInitializedApi() {
        var params = params();
        params.remoteAccessToken = null;
        assertThrows(IllegalArgumentException.class,
                () -> LauncherAPIInitializer.initializeClient(null, config(), params));
        assertNull(LauncherAPIHolder.get());
        assertNull(LauncherAPIHolder.core());
    }
}
