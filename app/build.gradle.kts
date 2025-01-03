plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    id("com.google.devtools.ksp") version "2.1.0-1.0.29"
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.captive_portal_analyzer_kotlin"
    compileSdk = 34

    defaultConfig {
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
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs += "-Xjvm-default=all"
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }


}

dependencies {

    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    // ViewModel utilities for Compose
    implementation (libs.androidx.lifecycle.viewmodel.compose)
    //lifecycle
    implementation (libs.androidx.lifecycle.runtime.ktx)
    //gson
    implementation (libs.gson)


    //navigation
    implementation (libs.androidx.navigation.compose.v272)

    // https://github.com/acsbendi/Android-Request-Inspector-WebView
    implementation ("com.github.acsbendi:Android-Request-Inspector-WebView:1.0.12")

    //pull to refresh
    implementation ("androidx.compose.foundation:foundation:1.6.0")

    //keep state of some variables across restarts
    implementation ("androidx.datastore:datastore-preferences:1.0.0")

    //Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    debugImplementation(libs.androidx.ui.tooling)
    ksp("androidx.room:room-compiler:2.6.1")

    //Toast
    implementation ("com.github.Spikeysanju:MotionToast:1.4")

    //coil for image loading
    implementation("io.coil-kt.coil3:coil-compose:3.0.4")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.4")

    //Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-storage")
    implementation(libs.firebase.firestore.ktx)


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