pluginManagement {
    repositories {
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        gradlePluginPortal()
    }
}

rootProject.name = "FrameSwitcher"

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version ("0.7.0")
}