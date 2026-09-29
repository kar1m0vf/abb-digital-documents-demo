plugins {
    `java-base`
    id("org.springframework.boot") version "4.1.1" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

allprojects {
    group = "az.abb"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    plugins.withType<JavaPlugin>().configureEach {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                val requestedVersion = providers.gradleProperty("javaVersion")
                    .orElse(JavaVersion.current().majorVersion)
                languageVersion.set(JavaLanguageVersion.of(requestedVersion.get().toInt()))
            }
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }

        tasks.withType<JavaCompile>().configureEach {
            options.encoding = "UTF-8"
            options.release.set(21)
            options.compilerArgs.add("-parameters")
        }
    }
}
