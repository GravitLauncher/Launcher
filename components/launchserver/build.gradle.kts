
plugins {
    id("application")
}


repositories {
    maven {
        url = uri("https://repo.clojars.org")
    }
}

val launcherInside = configurations.create("launcherInside") {
    isCanBeConsumed = false; isCanBeResolved = true
}

val proguardLibrary = configurations.create("proguardLibrary") {
    isCanBeConsumed = false; isCanBeResolved = true
}

dependencies {
    api(libs.log4j.api)
    api(libs.bouncycastle.bcpkix)
    implementation(libs.log4j.core)
    implementation(libs.log4j.slf4j.impl)
    implementation(libs.progressbar)
    implementation(libs.asm)
    implementation(libs.asm.tree)
    implementation(libs.jline.terminal)
    implementation(libs.jline.reader)
    implementation(libs.jansi)
    api(project(":components:launcher-api"))
    annotationProcessor(libs.log4j.core)
    launcherInside(project(mapOf("path" to ":components:launcher-runtime", "configuration" to "shadow")))
    proguardLibrary(libs.proguard)
}
tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf(
        "-Alog4j.graalvm.groupId=${project.group}",
        "-Alog4j.graalvm.artifactId=${project.name}"
    ))
}
tasks.withType<Test> {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
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

val copyProguardLibs = tasks.register("copyProguardLibs", Copy::class) {
    from(proguardLibrary)
    into(layout.buildDirectory.dir("proguard-libraries"))
}

application {
    evaluationDependsOn(":modules")
    evaluationDependsOn(":components:serverwrapper")

    mainClass = "pro.gravit.launchserver.LaunchServerStarter"
    mainModule = "launchserver"

    applicationDefaultJvmArgs += listOf(
        "--add-modules",
        "ALL-MODULE-PATH",
        "--add-modules",
        "java.net.http",
        "--add-modules",
        "java.xml",
        "--add-opens",
        "java.base/java.lang.invoke=launchserver",
        "--enable-native-access",
        "org.fusesource.jansi"
    )

    applicationDistribution.from(project(":components:launcher-runtime").tasks.named("copyLauncherLibs").map { it.outputs.files }) {
        into("launcher-libraries")
    }

    applicationDistribution.from(tasks.named("copyProguardLibs").map { it.outputs.files }) {
        into("proguard-libraries")
    }

    applicationDistribution.from(project(":modules").tasks.named("copyModules").map { it.outputs.files }) {
        into("modules")
    }

    applicationDistribution.from(project(":components:serverwrapper").tasks.named("fatJar").map { it.outputs.files }) {
        rename {"ServerWrapper.jar" }
    }

    applicationDistribution.from(project(":components:serverwrapper").tasks.named("inlineJar").map { it.outputs.files }) {
        rename { "ServerWrapperInline.jar" }
    }
}

distributions {
    main {
        contents {
            into("/")
        }
    }
}

tasks.assemble {
    dependsOn(copyProguardLibs)
}

tasks.build {
    dependsOn(tasks.distZip)
}
