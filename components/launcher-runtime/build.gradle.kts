plugins {
    id("com.gradleup.shadow")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.gravitlauncher.com/")
    }
}

val optional by configurations.creating {
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
    optional(libs.oshi)
}

val copyLauncherLibs by tasks.registering(Copy::class) {
    from(optional.resolve())
    into(layout.buildDirectory.dir("launcher-libraries"))
}

tasks.assemble {
    dependsOn(tasks.shadowJar, copyLauncherLibs)
}