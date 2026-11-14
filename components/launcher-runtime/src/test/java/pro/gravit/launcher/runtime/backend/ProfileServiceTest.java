package pro.gravit.launcher.runtime.backend;

import org.junit.jupiter.api.Test;
import pro.gravit.launcher.base.profiles.ClientProfileBuilder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProfileServiceTest {
    @Test
    void preparesProfilesIndexedByUuid() {
        UUID id = UUID.randomUUID();
        var profile = new ClientProfileBuilder().setUuid(id).createClientProfile();

        var prepared = new ProfileService().prepare(List.of(profile));

        assertEquals(profile, prepared.get(id));
    }

    @Test
    void rejectsDuplicateProfileIds() {
        UUID id = UUID.randomUUID();
        var first = new ClientProfileBuilder().setUuid(id).createClientProfile();
        var second = new ClientProfileBuilder().setUuid(id).createClientProfile();

        assertThrows(IllegalStateException.class, () -> new ProfileService().prepare(List.of(first, second)));
    }
}
