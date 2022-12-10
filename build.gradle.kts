import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("co.uzzu.dotenv.gradle") version "2.0.0"
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:7.3.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.21")
    }
}

allprojects {
    // Enable @OptIn annotation
    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_17.toString()

            freeCompilerArgs += listOf(
                "-opt-in=kotlin.RequiresOptIn"
            )
        }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
