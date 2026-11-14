plugins {
    id("com.gradleup.shadow")
}


repositories {
    maven {
        url = uri("https://maven.gravitlauncher.com/")
    }
}

val optional = configurations.create("optional") {
    isCanBeConsumed = false; isCanBeResolved = true;
}

configurations {
    compileOnly.get().extendsFrom(optional)
}

tasks.shadowJar {
    archiveClassifier.set("all")
    exclude("module-info.class")
    manifest {
        attributes(
            "Main-Class" to "pro.gravit.launcher.runtime.LauncherEngineWrapper"
        )
    }
}

dependencies {
    api(project(":components:launcher-client"))
    api(project(":components:launcher-start"))
    optional(libs.slf4j.simple)
    optional(libs.oshi.core)
    optional(libs.oshi.common)
}

val copyLauncherLibs = tasks.register("copyLauncherLibs", Copy::class) {
    from(optional)
    into(layout.buildDirectory.dir("launcher-libraries"))
}

tasks.assemble {
    dependsOn(tasks.shadowJar, copyLauncherLibs)
}
