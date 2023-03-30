plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.13.3"
}

group = "FrameSwitcher"
version = "4.0.0-223"

tasks {
    patchPluginXml {
        sinceBuild.set("223.0")
        untilBuild.set("")
        changeNotes.set(
            buildString {
                append("- Compatibility with IJ 2022.3").append("<br>")
            }
        )
    }

    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }


    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    buildSearchableOptions {
        enabled = false
    }
}

repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
    version.set("2023.1")
    type.set("IC") // Target IDE Platform

}


dependencies {

    // https://mvnrepository.com/artifact/com.google.guava/guava
    implementation("com.google.guava:guava:31.1-jre")
//    https://mvnrepository.com/artifact/org.jgroups/jgroups
    implementation("org.jgroups:jgroups:5.1.6.Final")
    implementation("uk.com.robust-it:cloning:1.9.12")
}

