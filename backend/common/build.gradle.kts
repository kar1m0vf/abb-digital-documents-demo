plugins {
    `java-library`
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))

    implementation(libs.spring.web)
    implementation(libs.spring.context)
    implementation(libs.jakarta.persistence)
    compileOnly(libs.jakarta.servlet)
}