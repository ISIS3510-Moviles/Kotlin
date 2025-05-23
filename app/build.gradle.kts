plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0"
    id("dagger.hilt.android.plugin")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
    alias(libs.plugins.google.firebase.firebase.perf)
    alias(libs.plugins.google.firebase.crashlytics)
    id("io.realm.kotlin")
}

configurations.all {

    exclude(group = "xmlpull", module = "xmlpull")
    exclude(group = "xpp3", module = "xpp3")
    exclude(group = "com.intellij", module = "annotations")
}

android {
    namespace = "com.example.campusbites"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.campusbites"
        minSdk = 29
        targetSdk = 35
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
    }
    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(libs.androidx.room.common.jvm)
    implementation(libs.androidx.room.compiler)
    implementation(libs.androidx.adapters)
    val roomVersion = "2.7.1"

    implementation("io.realm.kotlin:library-base:2.0.0")


    implementation(libs.androidx.room.runtime)
    kapt("androidx.room:room-compiler:$roomVersion")
    implementation(libs.androidx.room.ktx)

    implementation (libs.androidx.datastore.preferences)
    implementation (libs.androidx.credentials.vlatestversion)
    implementation (libs.googleid.vlatestversion)
    implementation(libs.androidx.credentials.play.services.auth.vlatestversion)
    implementation (libs.credentials.play.services.auth.v130)
    implementation (libs.firebase.auth.ktx.v2231)
    implementation(libs.maps.compose)
    implementation(libs.play.services.maps.v1820)
    implementation (libs.play.services.maps)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.play.services.auth)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.navigation.safe.args.generator)
    implementation(libs.play.services.location)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.firebase.perf)
    implementation(libs.firebase.perf.ktx)
    implementation(libs.firebase.crashlytics.ktx)
    implementation(libs.firebase.crashlytics)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.androidx.ui.text.google.fonts)
    implementation(libs.hilt.android.v254)
    kapt(libs.hilt.android.compiler.v254)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.retrofit)
    implementation(libs.converterGson)
    implementation(libs.okhttp)
    implementation(libs.coil.compose)
    implementation(libs.retrofit2.kotlinx.serialization.converter)
    implementation(libs.retrofit.v2110)
    implementation(libs.okhttp.v4110)
    implementation(libs.coil.compose.v240)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.accompanist.permissions)

}

kapt {
    correctErrorTypes = true
}

