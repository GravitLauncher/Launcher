package pro.gravit.launcher.profiles.optional.actions;

import pro.gravit.launcher.profiles.ClientProfile;

public class OptionalLibraryAction extends OptionalAction {
    public ClientProfile.ClientProfileLibrary[] libraries;

    public OptionalLibraryAction() {
    }

    public OptionalLibraryAction(ClientProfile.ClientProfileLibrary[] libraries) {
        this.libraries = libraries;
    }
}
