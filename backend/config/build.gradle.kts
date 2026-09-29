plugins {
    `java-library`
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))

    implementation(project(":common"))

    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.webmvc)
}