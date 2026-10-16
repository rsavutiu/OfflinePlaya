import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

// The cross-platform receiver: Compose Desktop on the JVM, so one codebase runs
// on Windows, macOS and Linux. See docs/plans/03-stream-to-pc.md. NOTE: this
// does NOT yet reuse :shared (domain/UI) — only :streamProtocol — so adding a
// jvm target to :shared can stay a separate, deliberate step.
kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":streamProtocol"))
    implementation(compose.desktop.currentOs)
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.ui)
    implementation(libs.kotlinx.coroutines.core)
    // Dispatchers.Main on Compose Desktop resolves to the Swing event thread.
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.kotlinx.serialization.json)
}

compose.desktop {
    application {
        mainClass = "com.offlineplaya.desktop.MainKt"
        // Package with a real JDK (jpackage), not whatever JVM runs Gradle —
        // Android Studio's bundled JBR has no jpackage. Resolve a full JDK 17
        // through Gradle toolchains (foojay auto-provisions one) so
        // createDistributable works on any dev machine and on CI.
        javaHome = javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(17))
        }.get().metadata.installationPath.asFile.absolutePath
        nativeDistributions {
            // App-image / installers per OS. Built per OS+arch on CI later.
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "OfflinePlayaReceiver"
            // Compose packaging requires MAJOR > 0 (DMG in particular); this is
            // the receiver's own version, independent of the Android app.
            packageVersion = "1.0.0"
        }
    }
}
