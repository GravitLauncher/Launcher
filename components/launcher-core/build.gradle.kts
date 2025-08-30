
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

dependencies {
    api(libs.gson)
    compileOnly(libs.jline.terminal)
    compileOnly(libs.jline.reader)
    compileOnly(libs.jansi)
    compileOnly(libs.slf4j)
    testRuntimeOnly(libs.jline.reader)
    testRuntimeOnly(libs.jline.terminal)
}