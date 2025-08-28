import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar.Companion.shadowJar
import java.util.jar.JarFile

plugins {
    id("application")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://repo.clojars.org")
    }
}

val launcherInside by configurations.creating {
    isCanBeConsumed = false; isCanBeResolved = true
}

val proguardLibrary by configurations.creating {
    isCanBeConsumed = false; isCanBeResolved = true
}

dependencies {
    api(libs.log4j.api)
    api(libs.netty.codec.http)
    api(libs.bouncycastle.bcpkix)
    api(libs.jjwt.api)
    implementation(libs.jjwt.impl)
    implementation(libs.jjwt.gson)
    implementation(libs.log4j.core)
    implementation(libs.netty.transport.epoll)
    implementation(libs.netty.transport.io.uring)
    implementation(libs.progressbar)
    implementation(libs.asm)
    implementation(libs.asm.tree)
    implementation(libs.db.hikari)
    implementation(libs.db.postgresql)
    implementation(libs.db.mysql)
    implementation(libs.db.mariadb)
    implementation(libs.db.h2)
    implementation(libs.jline.terminal)
    implementation(libs.jline.reader)
    api(project(":LauncherAPI"))
    annotationProcessor(libs.log4j.core)
    launcherInside(project(mapOf("path" to ":Launcher", "configuration" to "shadow")))
    proguardLibrary(libs.proguard)
}

tasks.jar {
    archiveClassifier.set("") // the main jar
    manifest {
        attributes(
            "Automatic-Module-Name" to "launchserver",
            "Implementation-Title" to "BackendSide",
            "Implementation-Version" to project.version
        )
    }

    from(launcherInside.elements.map { it.single().asFile }) {
        into(".")
        rename { "Launcher.jar" }
    }
}

val copyProguardLibs by tasks.registering(Copy::class) {
    from(proguardLibrary.resolve())
    into(layout.buildDirectory.dir("proguard-libraries"))
}

application {
    evaluationDependsOn(":modules")
    evaluationDependsOn(":ServerWrapper")

    mainClass = "pro.gravit.launchserver.LaunchServerStarter"
    mainModule = "launchserver"

    applicationDefaultJvmArgs += listOf(
        "--add-modules",
        "ALL-MODULE-PATH",
        "--add-modules",
        "java.net.http",
        "--add-opens",
        "java.base/java.lang.invoke=launchserver",
        "-Dlauncher.useSlf4j=true",
        "-Dio.netty.noUnsafe=true"
    )

    applicationDistribution.from(project(":Launcher").tasks.named("copyLauncherLibs").map { it.outputs.files }) {
        into("launcher-libraries")
    }

    applicationDistribution.from(tasks.named("copyProguardLibs").map { it.outputs.files }) {
        into("proguard-libraries")
    }

    applicationDistribution.from(project(":modules").tasks.named("copyModules").map { it.outputs.files }) {
        into("all-modules")
    }

    applicationDistribution.from(project(":ServerWrapper").tasks.named("fatJar").map { it.outputs.files }) {
        rename {"ServerWrapper.jar" }
    }

    applicationDistribution.from(project(":ServerWrapper").tasks.named("inlineJar").map { it.outputs.files }) {
        rename { "ServerWrapperInline.jar" }
    }
}

tasks.distZip {
    from(tasks.installDist.map { it.destinationDir }) {
        into("") // root of zip
    }
    dependsOn(project(":ServerWrapper").tasks["fatJar"],
        project(":ServerWrapper").tasks["inlineJar"],
        project(":Launcher").tasks["copyLauncherLibs"],
        project(":modules").tasks["copyModules"],
        copyProguardLibs)
}

tasks.assemble {
    dependsOn(copyProguardLibs)
}