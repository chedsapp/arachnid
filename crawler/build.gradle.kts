plugins {
    application
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(27)
    }
}

dependencies {
    implementation(project(":common"))
    implementation(libs.jsoup)
    implementation(libs.crawler.commons)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass = "arachnid.crawler.Main"
}

tasks.test {
    useJUnitPlatform()
}
