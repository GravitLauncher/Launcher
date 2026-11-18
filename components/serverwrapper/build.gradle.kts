import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

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

fun ShadowJar.configureWrapperArchive() {
    configurations.set(listOf(project.configurations.runtimeClasspath.get()))
    from(sourceSets.main.get().output)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    exclude("module-info.class", "META-INF/MANIFEST.SF")
    manifest.from(tasks.jar.get().manifest)
}

val fatJar = tasks.register<ShadowJar>("fatJar") {
    configureWrapperArchive()
    archiveClassifier.set("all")
}

tasks.named<ShadowJar>("shadowJar") {
    enabled = false
}

val inlineJar = tasks.register<ShadowJar>("inlineJar") {
    configureWrapperArchive()
    archiveClassifier.set("inline")
    exclude("com/google/**", "org/slf4j/**")
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
