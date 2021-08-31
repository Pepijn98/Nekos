plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("android.extensions")
}

object Versions {
    private const val versionMajor = 1
    private  const val versionMinor = 1
    private const val versionPatch = 2
    private val versionClassifier = null

    const val minSdk = 24
    const val targetSdk = 31

    fun generateVersionCode(): Int = minSdk * 10000000 + versionMajor * 10000 + versionMinor * 100 + versionPatch

    fun generateVersionName(): String {
        var versionName = "${versionMajor}.${versionMinor}.${versionPatch}"

        @Suppress("SENSELESS_COMPARISON")
        if (versionClassifier != null) {
            versionName += "-$versionClassifier"
        }
        return versionName
    }
}

android {
    compileSdk = Versions.targetSdk

    defaultConfig {
        applicationId = "dev.vdbroek.nekos"
        minSdk = Versions.minSdk
        targetSdk = Versions.targetSdk
        versionCode = Versions.generateVersionCode()
        versionName = Versions.generateVersionName()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    @Suppress("COMPATIBILITY_WARNING")
    buildTypes {
        release {
            isShrinkResources = true
            isMinifyEnabled = true
            isDebuggable = false
            isJniDebuggable = false
            isRenderscriptDebuggable = false
            isPseudoLocalesEnabled = false

            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }

        debug {
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

    sourceSets.all {
        java.srcDir("src/$name/kotlin")
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(kotlin("stdlib"))

    implementation("androidx.core:core-ktx:1.6.0")
    implementation("androidx.appcompat:appcompat:1.3.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.0")

    implementation("com.github.kittinunf.fuel:fuel:2.3.1")
    implementation("com.github.kittinunf.fuel:fuel-android:2.3.1")
    implementation("com.github.kittinunf.fuel:fuel-coroutines:2.3.1")
    implementation("com.github.kittinunf.fuel:fuel-gson:2.3.1")
    implementation("com.github.stfalcon:stfalcon-imageviewer:1.0.1")
    implementation("com.github.bumptech.glide:glide:4.11.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.11.0")

    implementation("com.google.android.material:material:1.4.0")
    implementation("com.google.code.gson:gson:2.8.6")
    implementation("com.facebook.fresco:fresco:1.9.0")
    implementation("com.squareup.picasso:picasso:2.71828")
    implementation("com.hendraanggrian:pikasso:0.2")

    implementation("de.hdodenhof:circleimageview:3.0.1")

    implementation("org.jetbrains.anko:anko:0.10.8")
    implementation("org.jetbrains.anko:anko-design:0.10.8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.4.2")

    testImplementation("junit:junit:4.13.1")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}
