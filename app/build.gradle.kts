plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
}

object Versions {
    private const val versionMajor = 2
    private const val versionMinor = 0
    private const val versionPatch = 0

    const val minSdk = 28
    const val targetSdk = 32

    fun generateVersionCode(): Int = minSdk * 10000000 + versionMajor * 10000 + versionMinor * 100 + versionPatch

    fun generateVersionName(): String = "$versionMajor.$versionMinor.$versionPatch"
}

android {
    namespace = "dev.vdbroek.nekos"
    compileSdk = Versions.targetSdk

    defaultConfig {
        applicationId = "dev.vdbroek.nekos"
        minSdk = Versions.minSdk
        targetSdk = Versions.targetSdk
        versionCode = Versions.generateVersionCode()
        versionName = Versions.generateVersionName()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            isDebuggable = false
            isJniDebuggable = false
            isRenderscriptDebuggable = false
            isPseudoLocalesEnabled = false
        }

        create("beta") {
            versionNameSuffix = "-BETA"
            applicationIdSuffix = ".beta"

            isShrinkResources = false
            isMinifyEnabled = false
            isDebuggable = false
            isJniDebuggable = false
            isRenderscriptDebuggable = false
            isPseudoLocalesEnabled = false

            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }

        getByName("debug") {
            versionNameSuffix = "-DEBUG"
            applicationIdSuffix = ".debug"

            isShrinkResources = false
            isMinifyEnabled = false
            isDebuggable = true // Set to false whenever publishing debug app to play console otherwise the AAB/APK will show as not signed.
            isJniDebuggable = true
            isRenderscriptDebuggable = true
            isPseudoLocalesEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    buildFeatures {
        compose = true
        viewBinding = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.1.1"
    }

    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.4.1")
    implementation("androidx.activity:activity-compose:1.4.0")
    implementation("androidx.compose.ui:ui:1.1.1")
    implementation("androidx.compose.ui:ui-tooling-preview:1.1.1")
    implementation("androidx.compose.material3:material3:1.0.0-alpha11")
    implementation("androidx.compose.material:material:1.1.1")
    implementation("androidx.navigation:navigation-compose:2.4.2")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("io.coil-kt:coil-compose:2.0.0")

    // Not used yet but seems like a good way to save logged in users
    implementation("androidx.room:room-runtime:2.4.2")
    kapt("androidx.room:room-compiler:2.4.2")
    implementation("androidx.room:room-ktx:2.4.2")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.1.1")
    debugImplementation("androidx.compose.ui:ui-tooling:1.1.1")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.1.1")
}
