package pro.gravit.launcher.base.request;

import pro.gravit.launcher.core.LauncherNetworkAPI;

public record UploadError(@LauncherNetworkAPI String error) {

}
