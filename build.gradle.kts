plugins {
    id("com.gradleup.shadow") version "9.2.2" apply false
    id("java")
    id("maven-publish")
}

group = "com.gravitlauncher.launcher"
version = "5.8.0-SNAPSHOT"


val myVersion = version
subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    project.version = myVersion

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
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

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    tasks.withType<Javadoc>().configureEach {
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
