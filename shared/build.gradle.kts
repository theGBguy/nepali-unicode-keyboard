import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Keyboard logic shared by the Android app and the iOS keyboard extension: transliteration,
// the Nepali calendar, numbers, units, smart suggestions, clipboard history and the key layouts.
// Pure Kotlin only; platform code (InputConnection, ML Kit, speech) stays in the apps.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
    android {
        namespace = "io.github.thegbguy.nepalikeyboard.shared"
        compileSdk = 36
        minSdk = 24
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        withHostTest {}
    }

    // The Mac input method (macosApp/) uses the same framework as the iOS keyboard
    listOf(iosArm64(), iosSimulatorArm64(), macosArm64(), macosX64()).forEach { target ->
        target.binaries.framework {
            baseName = "NepaliKeyboardShared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
