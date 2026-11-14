
plugins {
    id("com.gradleup.shadow")
}


tasks.jar {
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

val fatJar = tasks.register<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("fatJar") {
    configurations.set(listOf(project.configurations.runtimeClasspath.get()))
    from(sourceSets.main.get().output)
    archiveClassifier.set("all")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    exclude("module-info.class", "META-INF/MANIFEST.SF")
    manifest.from(tasks.jar.get().manifest)
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    enabled = false
}

val inlineJar = tasks.register<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("inlineJar") {
    configurations.set(listOf(project.configurations.runtimeClasspath.get()))
    from(sourceSets.main.get().output)
    archiveClassifier.set("inline")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    exclude("module-info.class", "META-INF/MANIFEST.SF")
    exclude("com/google/**")
    exclude("org/slf4j/**")
    manifest.from(tasks.jar.get().manifest)
}



repositories {
    maven {
        url = uri("https://maven.gravitlauncher.com/")
    }
}

dependencies {
    api(project(":components:launcher-client"))
    implementation(libs.slf4j.simple)
}

tasks.assemble {
    dependsOn(fatJar, inlineJar)
}
