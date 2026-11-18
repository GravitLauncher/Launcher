rootProject.name = "com.gravitlauncher.launcher"

fun includeChildren(parent: String) {
    rootDir.resolve(parent).listFiles().orEmpty()
        .filter { it.isDirectory }
        .filter { it.resolve("build.gradle.kts").isFile || it.resolve("build.gradle").isFile }
        .sortedBy { it.name }
        .forEach { include("$parent:${it.name}") }
}

includeChildren("components")
include("modules")
includeChildren("modules")
