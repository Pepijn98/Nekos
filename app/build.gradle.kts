object Versions {
    private const val versionMajor = 1
    private  const val versionMinor = 0
    private const val versionPatch = 0
    private val versionClassifier = null

    const val minSdkVersion = 24
    const val targetSdkVersion = 29

    fun generateVersionCode(): Int = minSdkVersion * 10000000 + versionMajor * 10000 + versionMinor * 100 + versionPatch

    fun generateVersionName(): String {
        var versionName = "${versionMajor}.${versionMinor}.${versionPatch}"

        @Suppress("SENSELESS_COMPARISON")
        if (versionClassifier != null) {
            versionName += "-$versionClassifier"
        }
        return versionName
    }
}

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("android.extensions")
}

android {
    compileSdkVersion(Versions.targetSdkVersion)
    defaultConfig {
        applicationId = "xyz.kurozero.nekosmoe"
        minSdkVersion(Versions.minSdkVersion)
        targetSdkVersion(Versions.targetSdkVersion)
        versionCode = Versions.generateVersionCode()
        versionName = Versions.generateVersionName()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.3.50")
    implementation("androidx.appcompat:appcompat:1.1.0")
    implementation("androidx.core:core-ktx:1.1.0")
    implementation("com.google.android.material:material:1.0.0")
    implementation("androidx.constraintlayout:constraintlayout:1.1.3")
    implementation("com.github.bumptech.glide:glide:4.9.0")
    implementation("org.jetbrains.anko:anko:0.10.8")
    implementation("com.github.kittinunf.fuel:fuel:2.2.0")
    implementation("com.google.code.gson:gson:2.8.5")
    implementation("com.facebook.fresco:fresco:1.9.0")
    implementation("com.github.stfalcon:frescoimageviewer:0.5.0")
    implementation("com.squareup.picasso:picasso:2.71828")
    implementation("com.hendraanggrian:pikasso:0.2")
    implementation("de.hdodenhof:circleimageview:3.0.1")
    testImplementation("junit:junit:4.12")
    androidTestImplementation("androidx.test.ext:junit:1.1.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.2.0")
}
