java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.gravitlauncher.com/")
    }
}

dependencies {
    api("com.gravitlauncher.launcher:socketbridge:1.0-SNAPSHOT")
    api(project(":components:launcher-api"))
}