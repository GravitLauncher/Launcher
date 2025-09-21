import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.gradleup.shadow")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

val fatJar by tasks.registering(ShadowJar::class) {
    from(sourceSets.main.get().output)
    configurations = listOf(project.configurations["runtimeClasspath"])
    archiveClassifier.set("all")
    exclude("module-info.class")
    manifest {
        attributes(
            "Main-Class" to "pro.gravit.launcher.server.ServerWrapper",
            "Premain-Class" to "pro.gravit.launcher.server.ServerAgent",
            "Can-Redefine-Classes" to "true",
            "Can-Retransform-Classes" to "true",
            "Can-Set-Native-Method-Prefix" to "true",
            "Multi-Release" to "true"
        )
    }
}

val inlineJar by tasks.registering(ShadowJar::class) {
    from(sourceSets.main.get().output)
    configurations = listOf(project.configurations["runtimeClasspath"])
    archiveClassifier.set("inline")
    exclude("module-info.class")
    exclude("com/google/**")
    manifest {
        attributes(
            "Main-Class" to "pro.gravit.launcher.server.ServerWrapper"
        )
    }
}



repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.gravitlauncher.com/")
    }
}

dependencies {
    api(project(":components:launcher-client"))
}

tasks.assemble {
    dependsOn(fatJar, inlineJar)
}