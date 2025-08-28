

subprojects {
    apply(plugin = "maven-publish")

    publishing {
        repositories {
            mavenLocal()
        }
        publications {
            var name = project.name
            create<MavenPublication>("maven") {
                groupId = "com.gravitlauncher.launcher"
                artifactId = name
                version = project.version as String?

                from(components["java"])
            }
        }
    }
}