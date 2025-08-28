import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.gradleup.shadow")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.shadowJar {
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

val taskInlineJar = tasks.register<ShadowJar>("inlineJar") {
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
}

dependencies {
    api(project(":LauncherClient"))
}

tasks.assemble {
    dependsOn(tasks.shadowJar, taskInlineJar)
}