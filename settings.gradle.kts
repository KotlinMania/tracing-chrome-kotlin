pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "tracing-chrome-kotlin"

val tracingSubscriberKotlinLocal = file("../tracing-subscriber-kotlin")
if (tracingSubscriberKotlinLocal.exists()) {
    includeBuild(tracingSubscriberKotlinLocal) {
        dependencySubstitution {
            substitute(module("io.github.kotlinmania:tracing-subscriber-kotlin"))
                .using(project(":"))
        }
    }
}

val serdeJsonKotlinLocal = file("../serde-json-kotlin")
if (serdeJsonKotlinLocal.exists()) {
    includeBuild(serdeJsonKotlinLocal) {
        dependencySubstitution {
            substitute(module("io.github.kotlinmania:serde-json-kotlin"))
                .using(project(":"))
        }
    }
}

val tracingKotlinLocal = file("../tracing-kotlin")
if (tracingKotlinLocal.exists()) {
    includeBuild(tracingKotlinLocal) {
        dependencySubstitution {
            substitute(module("io.github.kotlinmania:tracing-kotlin"))
                .using(project(":"))
        }
    }
}

val kmIoLocal = file("../km-io")
if (kmIoLocal.exists()) {
    includeBuild(kmIoLocal) {
        dependencySubstitution {
            substitute(module("io.github.kotlinmania:km-io"))
                .using(project(":"))
        }
    }
}

