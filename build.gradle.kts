import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google ()
        mavenCentral()
        maven(url = "https://jcenter.bintray.com")
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.1.0-alpha10")
        classpath(kotlin("gradle-plugin", version = "1.5.30"))
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jcenter.bintray.com")
        maven(url = "https://jitpack.io")
    }

    // -Xopt-in=kotlin.RequiresOptIn
    tasks.withType(KotlinCompile::class).all {
        kotlinOptions {
            freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
        }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
