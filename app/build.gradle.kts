plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
}

object Versions {
    private const val versionMajor = 2
    private const val versionMinor = 0
    private const val versionPatch = 8

    const val minSdk = 28
    const val targetSdk = 33

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
            storeFile = file(env.NEKOS_KEYSTORE.value)
            storePassword = env.NEKOS_KEYSTORE_PASS.value
            keyAlias = env.NEKOS_KEYSTORE_ALIAS.value
            keyPassword = env.NEKOS_KEYSTORE_PASS.value
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
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
            isShrinkResources = true
            isMinifyEnabled = true
            isDebuggable = false
            isJniDebuggable = false
            isRenderscriptDebuggable = false
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
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.core:core-splashscreen:1.0.0")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.5.1")

    // Compose libraries
    implementation("androidx.activity:activity-compose:1.6.1")
    implementation("androidx.compose.ui:ui:1.4.0-alpha03")
    implementation("androidx.compose.ui:ui-tooling-preview:1.4.0-alpha03")
    implementation("androidx.compose.material3:material3:1.1.0-alpha03")
    implementation("androidx.compose.material:material-icons-extended:1.4.0-alpha03")
    implementation("androidx.navigation:navigation-compose:2.5.3")

    // Google accompanist components
    implementation("com.google.accompanist:accompanist-flowlayout:0.28.0")
    implementation("com.google.accompanist:accompanist-swiperefresh:0.28.0")

    // Better fling behaviour modification
    implementation("com.github.iamjosephmj:Flinger:1.1.1")
    // Collapsing toolbar
    implementation("me.onebone:toolbar-compose:2.3.5")

    // HTTP Requests
    implementation("com.google.code.gson:gson:2.10")
    implementation("com.github.kittinunf.fuel:fuel:2.3.1")
    implementation("com.github.kittinunf.fuel:fuel-android:2.3.1")
    implementation("com.github.kittinunf.fuel:fuel-coroutines:2.3.1")
    implementation("com.github.kittinunf.fuel:fuel-gson:2.3.1")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")

    // Loading network images
    implementation("com.github.bumptech.glide:glide:4.13.2")
    kapt("com.github.bumptech.glide:compiler:4.13.2")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.4")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.4.0-alpha03")
    debugImplementation("androidx.compose.ui:ui-tooling:1.4.0-alpha03")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.4.0-alpha03")
}
