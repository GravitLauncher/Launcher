
rootProject.name = "com.gravitlauncher.launcher"

rootDir.resolve("components").listFiles().filter { it -> it.isDirectory }.forEach {
    if(it.resolve("build.gradle.kts").exists() || it.resolve("build.gradle").exists()) {
        include("components:"+it.name)
    }
}

include("modules")

rootDir.resolve("modules").listFiles().filter { it -> it.isDirectory }.forEach {
    if(it.resolve("build.gradle.kts").exists() || it.resolve("build.gradle").exists()) {
        include("modules:"+it.name)
    }
}