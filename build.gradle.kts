plugins {
    id("com.gradleup.shadow") version "9.2.2" apply false
    id("java")
    id("maven-publish")
}

group = "com.gravitlauncher.launcher"
version = "5.7.8"


val myVersion = version
subprojects {
    apply(plugin = "java")
    apply(plugin = "java-library")

    project.version = myVersion

    java {
        withSourcesJar()
        withJavadocJar()
    }
    repositories {
        mavenCentral()
    }
    dependencies {
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
        testImplementation(platform("org.junit:junit-bom:5.10.0"))
        testImplementation("org.junit.jupiter:junit-jupiter")
    }

    tasks.test {
        useJUnitPlatform()
    }

    tasks.javadoc {
        options {
            (this as CoreJavadocOptions).addBooleanOption("Xdoclint:none", true)
        }
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