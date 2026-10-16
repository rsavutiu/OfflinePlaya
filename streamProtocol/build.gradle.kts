plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
}

// Plain Kotlin/JVM so BOTH the Compose Desktop receiver and (later) the Android
// app can depend on it directly — the single source of truth for the wire
// format, with no need to add a jvm target to :shared. See
// docs/plans/03-stream-to-pc.md.
kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.kotlinx.serialization.json)

    // Pure-JVM unit tests for the wire format and the receiver ring buffer.
    // kotlin("test") auto-selects JUnit4 here (no useJUnitPlatform()).
    testImplementation(kotlin("test"))
}
