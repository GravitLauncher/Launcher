
repositories {
    maven {
        url = uri("https://maven.gravitlauncher.com/")
    }
}

dependencies {
    api("com.gravitlauncher.launcher:socketbridge:1.0.1-SNAPSHOT")
    api(project(":components:launcher-api"))
}
