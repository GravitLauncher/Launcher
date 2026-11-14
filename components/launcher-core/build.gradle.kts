


dependencies {
    api(project(":components:launcher-utils"))
    api(libs.gson)
    compileOnly(libs.jline.terminal)
    compileOnly(libs.jline.reader)
    compileOnly(libs.jansi)
    api(libs.slf4j)
    testRuntimeOnly(libs.jline.reader)
    testRuntimeOnly(libs.jline.terminal)
}
