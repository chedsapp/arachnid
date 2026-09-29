plugins {
    application
}

dependencies {
    implementation(project(":common"))
    implementation(libs.jsoup)
    implementation(libs.crawler.commons)
    testImplementation(libs.junit.jupiter)
}

application {
    mainClass = "arachnid.crawler.Main"
}
