import org.jetbrains.compose.ComposePlugin
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.jetbrainsCompose)
}

// Upload-key credentials are read from `androidApp/keystore.properties`
// (gitignored). When the file is absent — e.g. on a fresh checkout, in CI
// without secrets, or for a contributor who only ever builds debug — the
// release signing config is silently omitted so `assembleDebug` still
// works. Release builds then fall through to the unsigned-output path,
// which fails loudly with a clear "signingConfig not set" error.
val keystorePropsFile = rootProject.file("androidApp/keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

// Version is driven by CI on a tag push (see .github/workflows/release.yml):
// VERSION_CODE is the monotonic commit count, VERSION_NAME is the tag (sans
// leading "v"). Local/dev builds fall back to the committed defaults so
// `assembleDebug` works with no env set.
val appVersionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 2
val appVersionName = System.getenv("VERSION_NAME") ?: "0.3.0"

android {
    namespace = "com.offlineplaya.android"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.offlineplaya.android"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = appVersionCode
        versionName = appVersionName

        // The default AndroidJUnitRunner is enough — no custom runner needed
        // until we want Hilt/Koin lifecycle hooks for instrumented Koin tests.
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Make AGP package the native debug symbols (function names only —
        // FULL would also include line numbers, ~10x larger) for every .so
        // bundled into the release. The output zip is what Play Console
        // wants in App bundle explorer → Native debug symbols.
        //   File: androidApp/build/outputs/native-debug-symbols/release/
        //         native-debug-symbols.zip
        // Has no impact on your own code's crash deobfuscation (mapping.txt
        // is uploaded automatically inside the AAB); it only matters for
        // native crashes inside Media3/SQLite/etc.
        ndk {
            debugSymbolLevel = "SYMBOL_TABLE"
        }
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    // Lint Vital runs by default before every release build and tries to
    // download check-definition updates over HTTPS. On this machine the
    // JBR truststore rejects the proxy's MITM cert with "PKIX path
    // building failed", which blocks the release build. The check
    // definitions bundled in AGP are enough; turn off the network-going
    // pre-release lint pass.
    lint {
        checkReleaseBuilds = false
    }

    buildFeatures {
        compose = true
    }

    // Strip the ~70 locales that AndroidX / Compose-Material libraries bundle
    // in their res/values-XX tables — that's pure framework chrome (default
    // widget a11y text etc.) and was ~138 KB of dead weight.
    //
    // This does NOT affect the app's own translations: those live in
    // shared/composeResources and ship as assets (assets/composeResources/
    // values-XX/strings.commonMain.cvr), which Compose Resources resolves at
    // runtime by itself — independent of this res/ locale filter. Verified by
    // unzipping the release AAB: values-ru/-ja/-de/... assets are all present.
    androidResources {
        localeFilters += "en"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // androidx.appcompat is dragged in transitively by Koin's android
    // artifacts (both directly and via :shared). This app is Compose-only
    // with a single Activity — AppCompat's resources (the abc_*.xml
    // drawables and layouts) and dex are dead weight. Excluding here at
    // the final-app level catches every transitive path.
    implementation(project(":shared")) {
        exclude(group = "androidx.appcompat")
    }

    val compose = ComposePlugin.Dependencies(project)
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.ui)
    implementation(compose.materialIconsExtended)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.documentfile)

    // Koin's android artifacts transitively bring in androidx.appcompat for
    // a Fragment/AppCompatActivity scope helper we don't use — this app is
    // Compose-only with a single Activity. The exclusion strips the
    // AppCompat resources (the abc_*.xml drawables/layouts in the bundle)
    // and dex.
    implementation(libs.koin.android) {
        exclude(group = "androidx.appcompat")
    }
    implementation(libs.koin.androidxCompose) {
        exclude(group = "androidx.appcompat")
    }
    implementation(libs.koin.compose)

    implementation(libs.coil.compose)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.common)

    implementation(libs.androidx.work.runtime)
    implementation(libs.lifecycle.process)

    // Compose Preview: annotation (always) + renderer (debug only, for Android Studio)
    implementation(compose.components.uiToolingPreview)
    debugImplementation(compose.uiTooling)

    // Instrumented tests — exercise the real Android stack (SAF, MediaStore,
    // Jaudiotagger over real files) against the fixture audio committed under
    // src/androidTest/assets/fixtures.
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
