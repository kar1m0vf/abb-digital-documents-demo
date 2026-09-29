plugins {
    `java-library`
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))

    implementation(project(":common"))
    implementation(project(":embassy"))
    implementation(project(":order"))
    implementation(project(":customer"))
    implementation(project(":notification"))

    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.webmvc)
    implementation(libs.jackson.annotations)
    implementation(libs.swagger.annotations)

    testImplementation(libs.spring.boot.starter.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}