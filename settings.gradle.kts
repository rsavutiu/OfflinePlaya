rootProject.name = "OfflinePlaya"

include(":androidApp")
include(":shared")
// Stream-to-PC (docs/plans/03-stream-to-pc.md): the wire-format module is plain
// Kotlin/JVM so the Android app can reuse it later without adding a jvm target
// to :shared; the receiver is a Compose Desktop (JVM) app for Win/macOS/Linux.
include(":streamProtocol")
include(":desktopApp")

pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.android.kotlin.multiplatform.library") {
                useModule("com.android.tools.build:gradle:${requested.version}")
            }
        }
    }
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}
