# R8 / ProGuard rules for the release build.
#
# Most libraries this project depends on (Compose, Media3, OkHttp, Coil,
# Koin, kotlinx-coroutines) ship their own consumer rules — AGP picks
# those up automatically, no entries needed here. Only the things below
# need manual hints.

# ── kotlinx.serialization ──────────────────────────────────────────────
# The compiler plugin generates a synthetic `Companion.serializer()` for
# each @Serializable class. R8 doesn't see the call site (it's wired up
# at runtime by Json.encodeToString/decodeFromString), so without keep
# rules the companion gets stripped and decode throws
# SerializationException at runtime.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class **$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-keep,includedescriptorclasses class com.offlineplaya.**$$serializer { *; }
-keepclassmembers class com.offlineplaya.** {
    *** Companion;
}
-keepclasseswithmembers class com.offlineplaya.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Jaudiotagger (writes embedded album art / tags) ────────────────────
# Uses reflection to instantiate ID3 frame body classes by name. Stripping
# any of them turns "save tag" into a NoSuchMethodException at runtime.
-keep class org.jaudiotagger.** { *; }
-dontwarn org.jaudiotagger.**

# ── Compose @Preview ───────────────────────────────────────────────────
# Not strictly required for release (Studio renders previews against the
# debug variant) but keeping the annotation lets release builds opened
# in Studio still surface previews.
-keep class androidx.compose.ui.tooling.preview.Preview { *; }
-keep @androidx.compose.ui.tooling.preview.Preview class *

# ── Stack-trace readability ────────────────────────────────────────────
# Keep file:line info so Play Console crash reports stay decodable with
# the mapping file Gradle uploads.
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
