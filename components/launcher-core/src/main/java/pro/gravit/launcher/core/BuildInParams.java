package pro.gravit.launcher.core;

import pro.gravit.utils.Version;

public class BuildInParams {
    private static volatile Version version;
    private static volatile String projectName;

    public static Version getVersion() {
        return version;
    }

    public static void setVersion(Version version) {
        BuildInParams.version = version;
    }

    public static String getProjectName() {
        return projectName;
    }

    public static void setProjectName(String projectName) {
        BuildInParams.projectName = projectName;
    }
}
