plugins {
    id("com.gradleup.shadow") version "9.0.2" apply false
    id("java")
}

group = "com.gravitlauncher.launcher"
version = "1.0-SNAPSHOT"



subprojects {
    apply(plugin = "java")
    apply(plugin = "java-library")
    java {
        withSourcesJar()
        withJavadocJar()
    }
    dependencies {
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
        testImplementation(platform("org.junit:junit-bom:5.10.0"))
        testImplementation("org.junit.jupiter:junit-jupiter")
    }

    tasks.test {
        useJUnitPlatform()
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}