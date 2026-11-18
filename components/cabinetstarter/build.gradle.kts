import org.gradle.api.file.ArchiveOperations
import javax.inject.Inject

interface CabinetStarterArchives {
    @get:Inject
    val archives: ArchiveOperations
}

val embedded = sourceSets.create("embedded")

val launcherBootstrap = configurations.create("launcherBootstrap") {
    isCanBeConsumed = false
    isTransitive = false
}

dependencies {
    add(launcherBootstrap.name, project(":components:launcher-utils"))
    compileOnly("org.springframework:spring-context:7.0.0")
    compileOnly("org.springframework.boot:spring-boot:4.0.6")
    compileOnly(project(":components:launcher-utils"))
    add(embedded.compileOnlyConfigurationName, project(":components:launchserver"))
    add(embedded.compileOnlyConfigurationName, libs.log4j.core)
}

tasks.jar {
    archiveFileName.set("cabinetstarter.jar")
    val archives = objects.newInstance<CabinetStarterArchives>().archives
    from(launcherBootstrap.elements.map { archives.zipTree(it.single().asFile) }) {
        include(
            "pro/gravit/utils/launch/**",
            "pro/gravit/utils/helper/IOHelper*.class",
            "pro/gravit/utils/helper/JVMHelper*.class",
            "pro/gravit/utils/helper/HackHelper*.class",
            "pro/gravit/utils/helper/VerifyHelper*.class"
        )
    }
}

val embeddedJar = tasks.register<Jar>("embeddedJar") {
    archiveFileName.set("cabinetstarter-embedded.jar")
    from(embedded.output)
}

evaluationDependsOn(":components:launchserver")
val launchServerDist = project(":components:launchserver").tasks.named<Sync>("installDist")

val cabinetDist = tasks.register<Sync>("installDist") {
    group = "distribution"
    description = "Stages CabinetStarter and its isolated LaunchServer runtime for SimpleCabinet"
    into(layout.buildDirectory.dir("install/cabinetstarter"))
    from(tasks.jar) { into("modules") }
    from(launchServerDist.map { it.destinationDir }) {
        into("launchserver")
        exclude("bin/**", "lib/log4j-slf4j2-impl-*.jar", "lib/slf4j-api-*.jar")
    }
    from(embeddedJar) { into("launchserver/lib") }
    dependsOn(launchServerDist)
}

tasks.assemble { dependsOn(cabinetDist) }
