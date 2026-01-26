
plugins {
    id("com.gradleup.shadow")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
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

val fatJar by tasks.registering(Jar::class) {
    dependsOn(":components:launcher-core:jar")
    dependsOn(":components:launcher-api:jar")
    dependsOn(":components:launcher-client:jar")
    from(project.configurations["runtimeClasspath"]
        .map({ if (it.isDirectory) it else zipTree(it) }))
    from(sourceSets.main.get().output)
    archiveClassifier.set("all")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    exclude("module-info.class", "META-INF/MANIFEST.SF")
}

val inlineJar by tasks.registering(Jar::class) {
    dependsOn(":components:launcher-core:jar")
    dependsOn(":components:launcher-api:jar")
    dependsOn(":components:launcher-client:jar")
    from(project.configurations["runtimeClasspath"]
        .map({ if (it.isDirectory) it else zipTree(it) }))
    from(sourceSets.main.get().output)
    archiveClassifier.set("inline")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    exclude("module-info.class")
    exclude("com/google/**")
    exclude("org/slf4j/**")
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