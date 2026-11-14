package pro.gravit.launchserver.profiles;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pro.gravit.launcher.core.hasher.HashedDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemoteProfilesProviderTest {
    @TempDir
    Path temporary;

    @Test
    void advertisesOnlyImplementedCasOperations() {
        RemoteProfilesProvider provider = new RemoteProfilesProvider();
        try {
            assertTrue(provider.capabilities().actions().contains(ProfilesProvider.ProfileAction.ProfileActionType.UPLOAD));
            assertFalse(provider.capabilities().actions().contains(ProfilesProvider.ProfileAction.ProfileActionType.DELETE));
            assertFalse(provider.capabilities().namedRevisions());
        } finally {
            provider.close();
        }
    }

    @Test
    void rejectsUnsupportedActionsAndRevisionsBeforeNetworkWork() {
        RemoteProfilesProvider provider = new RemoteProfilesProvider();
        try {
            ProfilesProvider.ProfileAction delete = new ProfilesProvider.ProfileAction(
                    ProfilesProvider.ProfileAction.ProfileActionType.DELETE, null, "client/file", null, null, false);
            assertThrows(UnsupportedOperationException.class,
                    () -> provider.pushUpdate(null, "main", null, List.of(delete), null, List.of()));
            assertThrows(UnsupportedOperationException.class, () -> provider.get("profile", "release-2"));
        } finally {
            provider.close();
        }
    }

    @Test
    void rejectsTraversalInManifestPathsBeforeStartingDownloads() {
        RemoteProfilesProvider provider = new RemoteProfilesProvider();
        try {
            HashedDir empty = new HashedDir();
            RemoteProfilesProvider.HttpProfile profile = new RemoteProfilesProvider.HttpProfile(null, empty, empty);
            assertThrows(IOException.class,
                    () -> provider.download(profile, Map.of("../escape", temporary.resolve("target")), false));
        } finally {
            provider.close();
        }
    }
}
