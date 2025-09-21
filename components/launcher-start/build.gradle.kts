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
    api(project(":components:launcher-client"))
}