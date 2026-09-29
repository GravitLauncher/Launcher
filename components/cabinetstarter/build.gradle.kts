
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    compileOnly("com.gravitlauncher:simplecabinet:0.0.1-SNAPSHOT")
    compileOnly("org.springframework:spring-context:7.0.0")
    compileOnly("org.springframework:spring-web:7.0.0")
    compileOnly("org.springframework.boot:spring-boot:4.0.6")
    compileOnly("org.springframework.security:spring-security-config:7.0.0")
    compileOnly("org.springframework.security:spring-security-web:7.0.0")
    compileOnly(libs.slf4j)
    implementation(project(":components:launchserver"))
    testRuntimeOnly(libs.jline.reader)
    testRuntimeOnly(libs.jline.terminal)
}