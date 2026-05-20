plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.materialIconsExtended)

                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kermit)

                implementation(libs.sqldelight.runtime)
                implementation(libs.sqldelight.coroutines)
                implementation(libs.sqldelight.primitive.adapters)

                implementation(libs.koin.core)
                implementation(libs.koin.compose)

                implementation(libs.coil.compose)

                implementation(libs.kotlinx.serialization.json)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(libs.sqldelight.driver.jvm)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.android)
                implementation(libs.sqldelight.driver.android)
                implementation(compose.uiTooling)
                implementation(compose.preview)
                implementation(libs.koin.android)
                implementation(libs.koin.androidxCompose)

                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.lifecycle.viewmodel)
                implementation(libs.androidx.lifecycle.viewmodel.compose)
                implementation(libs.androidx.lifecycle.runtime.compose)
                implementation(libs.androidx.navigation.compose)
                implementation(libs.androidx.documentfile)
                implementation(libs.androidx.datastore)

                implementation(libs.androidx.media3.exoplayer)
                implementation(libs.androidx.media3.session)
                implementation(libs.androidx.media3.common)
                implementation(libs.androidx.media3.ui)

                implementation(libs.jaudiotagger)
                implementation(libs.okhttp)
                implementation(libs.androidx.work.runtime)
            }
        }
    }
}

android {
    namespace = "com.offlineplaya.shared"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

sqldelight {
    databases {
        create("OfflinePlayaDatabase") {
            packageName.set("com.offlineplaya.shared.database")
            srcDirs.setFrom("src/commonMain/sqldelight")
        }
    }
}
