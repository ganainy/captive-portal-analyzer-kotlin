import com.android.build.api.dsl.BuildType

// Helper function to make adding buildConfigFields cleaner
fun BuildType.buildConfigStringField(name: String, value: String) {
    buildConfigField("String", name, "\"$value\"")
}

fun BuildType.buildConfigBooleanField(name: String, value: Boolean) {
    buildConfigField("Boolean", name, value.toString())
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    id("com.google.devtools.ksp") version "2.1.0-1.0.29"
    id("com.google.gms.google-services")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

android {
    namespace = "com.example.captive_portal_analyzer_kotlin"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.captive_portal_analyzer_kotlin"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigBooleanField("IS_APP_IN_DEBUG_MODE", false)
            buildConfigBooleanField("DEBUG_USE_TESTING_WEBVIEW", false)
            buildConfigBooleanField("DEBUG_SKIP_CHECKLIST_SCREEN", false)
            buildConfigBooleanField("DEBUG_SKIP_PCAP_SETUP_SCREEN", false)
        }

        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigBooleanField("IS_APP_IN_DEBUG_MODE", false)
            buildConfigBooleanField("DEBUG_USE_TESTING_WEBVIEW", false)
            buildConfigBooleanField("DEBUG_SKIP_CHECKLIST_SCREEN", false)
            buildConfigBooleanField("DEBUG_SKIP_PCAP_SETUP_SCREEN", false)

            signingConfig = signingConfigs.getByName("debug")
        }

        // ---  custom build type here for testing purposes ---
        create("stagingDebug") {
            //Inherit from debug and override
            initWith(buildTypes.getByName("debug"))

            // Override or add specific flags
            buildConfigBooleanField("IS_APP_IN_DEBUG_MODE", true)
            buildConfigBooleanField("DEBUG_USE_TESTING_WEBVIEW", true)
            buildConfigBooleanField("DEBUG_SKIP_CHECKLIST_SCREEN", true)
            buildConfigBooleanField("DEBUG_SKIP_PCAP_SETUP_SCREEN", true)

        }
    }


    secrets {
        propertiesFileName = "secrets.properties"
        defaultPropertiesFileName = "local.defaults.properties"
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
    implementation("io.coil-kt:coil-video:2.6.0")

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

    //core library
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

    // to enable preview function in stagingDebug build type
    "stagingDebugImplementation"(libs.ui.tooling)
}

