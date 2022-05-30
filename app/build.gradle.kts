plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
}

object Versions {
    private const val versionMajor = 2
    private const val versionMinor = 0
    private const val versionPatch = 4

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

        setProperty("archivesBaseName", "${namespace}_${Versions.generateVersionName()}")
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("default") {
            storeFile = file(System.getenv("NEKOS_KEYSTORE"))
            storePassword = System.getenv("NEKOS_KEYSTORE_PASS")
            keyAlias = System.getenv("NEKOS_KEYSTORE_ALIAS")
            keyPassword = System.getenv("NEKOS_KEYSTORE_PASS")
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = false
            isJniDebuggable = false
            isRenderscriptDebuggable = false
            isPseudoLocalesEnabled = false
            signingConfig = signingConfigs.getByName("default")

            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }

        create("uncensored") {
            versionNameSuffix = "-UNCENSORED"
            applicationIdSuffix = ".uncensored"
            isShrinkResources = false
            isMinifyEnabled = false
            isDebuggable = true
            isJniDebuggable = true
            isRenderscriptDebuggable = true
            isPseudoLocalesEnabled = false
            signingConfig = signingConfigs.getByName("default")

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
            signingConfig = signingConfigs.getByName("default")

            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
        kotlinCompilerExtensionVersion = "1.2.0-beta02"
    }

    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Android libraries
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.core:core-splashscreen:1.0.0-rc01")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.4.1")

    // Compose libraries
    implementation("androidx.activity:activity-compose:1.4.0")
    implementation("androidx.compose.ui:ui:1.2.0-beta02")
    implementation("androidx.compose.ui:ui-tooling-preview:1.2.0-beta02")
    implementation("androidx.compose.material3:material3:1.0.0-alpha12")
    implementation("androidx.compose.material:material-icons-extended:1.2.0-beta02")
    implementation("androidx.navigation:navigation-compose:2.4.2")

    // Google accompanist components
    implementation("com.google.accompanist:accompanist-flowlayout:0.24.9-beta")
    implementation("com.google.accompanist:accompanist-swiperefresh:0.24.9-beta")

    // Better fling behaviour modification
    implementation("com.github.iamjosephmj:Flinger:1.1.1")
    // Until lazy staggered grid is officially supported this is the best implementation I could find
    // It is on the roadmap https://developer.android.com/jetpack/androidx/compose-roadmap
    // So surely some day it will be added to jetpack compose :)
    implementation("com.github.nesyou01:LazyStaggeredGrid:1.1")
    // Collapsing toolbar
    implementation("me.onebone:toolbar-compose:2.3.3")

    // HTTP Requests
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("com.github.kittinunf.fuel:fuel:2.3.1")
    implementation("com.github.kittinunf.fuel:fuel-android:2.3.1")
    implementation("com.github.kittinunf.fuel:fuel-coroutines:2.3.1")
    implementation("com.github.kittinunf.fuel:fuel-gson:2.3.1")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")

    // Loading network images
    implementation("com.github.bumptech.glide:glide:4.13.2")
    kapt("com.github.bumptech.glide:compiler:4.13.2")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.2.0-beta02")
    debugImplementation("androidx.compose.ui:ui-tooling:1.2.0-beta02")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.2.0-beta02")
}
