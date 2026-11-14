package pro.gravit.launcher.runtime.backend;

import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launcher.core.api.features.ProfileFeatureAPI;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Performs profile snapshot preparation independently of backend state publication. */
final class ProfileService {
    Map<UUID, ProfileFeatureAPI.ClientProfile> prepare(List<ProfileFeatureAPI.ClientProfile> profiles) {
        Map<UUID, ProfileFeatureAPI.ClientProfile> result = profiles.stream()
                .collect(Collectors.toMap(ProfileFeatureAPI.ClientProfile::getUUID, profile -> profile));
        for (var profile : result.values()) {
            if (profile instanceof ClientProfile clientProfile) clientProfile.updateOptionalGraph();
        }
        return result;
    }
}
