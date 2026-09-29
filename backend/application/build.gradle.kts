plugins {
    `java`
    id("org.springframework.boot")
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))

    implementation(project(":common"))
    implementation(project(":config"))
    implementation(project(":customer"))
    implementation(project(":embassy"))
    implementation(project(":auth"))
    implementation(project(":order"))
    implementation(project(":portal"))
    implementation(project(":notification"))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.liquibase)
    implementation(libs.springdoc)

    runtimeOnly(libs.postgresql)
    runtimeOnly("com.h2database:h2")

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.security.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
