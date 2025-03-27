
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    id("com.google.devtools.ksp") version "2.1.0-1.0.29"
    id("com.google.gms.google-services")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

android {
    ndkVersion = "26.3.11579264"
    namespace = "com.example.captive_portal_analyzer_kotlin"
    compileSdk = 35

    defaultConfig {
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
        }
        externalNativeBuild {
            cmake {
                // Optional: Add arguments if needed (copy from Project A if necessary)
                // arguments "-DANDROID_STL=c++_shared"
                // Optional: Specify specific ABIs if needed
                // abiFilters 'armeabi-v7a', 'arm64-v8a'
            }
        }
        applicationId = "com.example.captive_portal_analyzer_kotlin"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
            /*applicationIdSuffix = ".debug"*/
            versionNameSuffix = "-beta"

            packagingOptions {
                if (project.hasProperty("doNotStrip")) {
                    doNotStrip("**/libpcapd.so")
                    doNotStrip("**/libcapture.so")
                }
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += "-Xjvm-default=all"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

/*    externalNativeBuild {
        cmake {
            path = file("src/main/jni/CMakeLists.txt")
            version = "3.22.1"
        }
    }*/




    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }


}



dependencies {

    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    // ViewModel utilities for Compose
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    //lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    //gson
    implementation(libs.gson)


    //navigation
    implementation(libs.androidx.navigation.compose.v272)

    // https://github.com/acsbendi/Android-Request-Inspector-WebView
    implementation(libs.android.request.inspector.webview)

    //pull to refresh
    implementation(libs.androidx.foundation)

    //keep state of some variables across restarts
    implementation(libs.androidx.datastore.preferences)

    //Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.preference.ktx)
    debugImplementation(libs.androidx.ui.tooling)
    ksp(libs.androidx.room.compiler)

    //coil for image loading
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    //Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.firestore.ktx)

    //Gemini sdk
    implementation(libs.generativeai)

    //compose markdown
    implementation(libs.compose.markdown)

    //time ago
    implementation(libs.timeago)

    // Brotli dependency
    implementation(libs.brotli.dec)

    //MaxMind GeoIP2
    implementation(libs.db.maxmind.db)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.runtime.android)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.material3.android)
    implementation(libs.firebase.common.ktx)
    implementation(libs.androidx.ui.tooling.preview.android)
    implementation(libs.androidx.navigation.compose)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

