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
# Narrow keep rules — Jaudiotagger ships ~600 classes (every ID3 frame
# variant, every audio-container reader/writer, plus tests). R8 can trace
# almost everything we reach statically; only the reflectively loaded
# pieces need explicit keeps:
#
#   1. ID3v2 frame body classes — `AbstractID3v2Frame.readBody()` does
#      `Class.forName("...FrameBody" + frameId)` then invokes the public
#      ctor by name. Strip any of them and tag-read throws at runtime.
#   2. Per-format AudioFileReader/Writer SPI lookup. AudioFileIO holds a
#      hardcoded registry that R8 sees, but the registry entries point at
#      no-arg constructors that R8 would otherwise inline away.
#   3. The bits of public API surface we hold direct references to
#      (AudioFileIO, FieldKey, FlacTag, VorbisCommentTag, ArtworkFactory,
#      MetadataBlockDataPicture, TagOptionSingleton).
#
# If a tag-write crashes with NoSuchMethodException or ClassNotFoundException
# later, the missing class belongs in this list. The blanket
# `-keep class org.jaudiotagger.** { *; }` is the fallback.
-keep class org.jaudiotagger.tag.id3.framebody.** { *; }
-keep class org.jaudiotagger.tag.id3.valuepair.** { *; }
-keep class org.jaudiotagger.tag.reference.** { *; }
-keep class * extends org.jaudiotagger.audio.generic.AudioFileReader { <init>(); }
-keep class * extends org.jaudiotagger.audio.generic.AudioFileWriter { <init>(); }
-keep class org.jaudiotagger.audio.AudioFileIO { *; }
-keep class org.jaudiotagger.tag.TagOptionSingleton { *; }
-keep class org.jaudiotagger.tag.FieldKey { *; }
-keep class org.jaudiotagger.tag.flac.** { *; }
-keep class org.jaudiotagger.tag.vorbiscomment.** { *; }
-keep class org.jaudiotagger.tag.images.** { *; }
-keep class org.jaudiotagger.audio.flac.metadatablock.** { *; }
-dontwarn org.jaudiotagger.**

# ── Compose @Preview ───────────────────────────────────────────────────
# We don't keep previewed composables in release — Android Studio renders
# previews against the debug variant, so release has no reason to retain
# every @Preview function (and the PreviewTheme / sample data they pull
# in). Dropping the wildcard keep lets R8 prune the whole preview graph.

# ── Stack-trace readability ────────────────────────────────────────────
# Keep file:line info so Play Console crash reports stay decodable with
# the mapping file Gradle uploads.
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
