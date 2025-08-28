rootProject.name = "gravitlauncher"

include("LauncherCore")
include("LauncherAPI")
include("LauncherClient")
include("LauncherStart")
include("ServerWrapper")
include("Launcher")
include("LaunchServer")
include("modules")

rootDir.resolve("modules").listFiles().filter { it -> it.isDirectory }.forEach {
    if(it.resolve("build.gradle.kts").exists() || it.resolve("build.gradle").exists()) {
        include("modules:"+it.name)
    }
}