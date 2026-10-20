import com.android.build.api.artifact.SingleArtifact
import java.io.File
import java.util.Properties

// Upload key for Play: read from git-ignored keystore.properties, or PLAY_STORE_* env vars on CI.
val keystoreFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply { if (keystoreFile.isFile) keystoreFile.inputStream().use(::load) }
fun signingValue(name: String, env: String): String? =
    providers.environmentVariable(env).orNull ?: keystoreProps.getProperty(name)?.takeIf(String::isNotBlank)
val uploadStoreFile = signingValue("storeFile", "PLAY_STORE_STORE_FILE")
val uploadStorePassword = signingValue("storePassword", "PLAY_STORE_STORE_PASSWORD")
val uploadKeyAlias = signingValue("keyAlias", "PLAY_STORE_KEY_ALIAS")
val uploadKeyPassword = signingValue("keyPassword", "PLAY_STORE_KEY_PASSWORD")
val isUploadSigningConfigured =
    listOf(uploadStoreFile, uploadStorePassword, uploadKeyAlias, uploadKeyPassword).all { !it.isNullOrBlank() }

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.thegbguy.nepalikeyboard"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.thegbguy.nepalikeyboard"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val uploadSigning = if (isUploadSigningConfigured) {
        signingConfigs.create("upload") {
            storeFile = rootProject.file(requireNotNull(uploadStoreFile))
            storePassword = uploadStorePassword
            keyAlias = uploadKeyAlias
            keyPassword = uploadKeyPassword
        }
    } else {
        null
    }

    buildTypes {
        debug {
            // Installs beside the Play build. Debug names live in src/debug/res.
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = uploadSigning
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    androidResources {
        // Only the app's own languages; drops library translations for languages we don't support.
        localeFilters += listOf("en", "ne")
    }
    bundle {
        // Ship every language in every install: the in-app language setting can pick Nepali
        // on a phone set to English, and the strings are tiny.
        language {
            enableSplit = false
        }
    }
}

// The privacy promise ("no internet access, so typed text can't leave the phone") rests on the
// merged manifest. Fail the build if any dependency adds the INTERNET permission.
abstract class CheckNoInternetPermission : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val mergedManifest: RegularFileProperty

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun verify() {
        val manifest = mergedManifest.get().asFile.readText()
        check(!manifest.contains("\"android.permission.INTERNET\"")) {
            "The merged manifest declares android.permission.INTERNET. The keyboard's privacy claim " +
                "depends on having no network access: find the dependency that added it (manifest-merger " +
                "report) and remove it, or update the privacy policy, Data safety and store listing first."
        }
        report.get().asFile.writeText("OK: no INTERNET permission\n")
    }
}

androidComponents {
    onVariants { variant ->
        val suffix = variant.name.replaceFirstChar(Char::uppercase)
        val checkTask = tasks.register<CheckNoInternetPermission>("check${suffix}NoInternetPermission") {
            mergedManifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
            report.set(layout.buildDirectory.file("reports/no-internet/${variant.name}.txt"))
        }
        tasks.configureEach {
            if (name == "assemble$suffix" || name == "bundle$suffix" || name == "check") {
                dependsOn(checkTask)
            }
        }
    }
}

// Fails fast when bundleRelease would otherwise produce an unsigned bundle.
val validateUploadSigning by tasks.registering {
    group = "build"
    description = "Checks that the Play upload key is configured."
    // Plain values only: the configuration cache can't serialize references to the build script.
    val configured = isUploadSigningConfigured
    val storePath = uploadStoreFile?.let { rootProject.file(it).absolutePath }
    doLast {
        check(configured) {
            "Upload signing is not configured. Copy keystore.properties.example to keystore.properties and fill it in, " +
                "or set PLAY_STORE_STORE_FILE, PLAY_STORE_STORE_PASSWORD, PLAY_STORE_KEY_ALIAS and PLAY_STORE_KEY_PASSWORD."
        }
        check(File(requireNotNull(storePath)).isFile) { "Upload keystore not found: $storePath" }
    }
}
tasks.matching { it.name == "bundleRelease" }.configureEach { dependsOn(validateUploadSigning) }

dependencies {
    // Core Android dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose dependencies
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Lifecycle for Compose in IME
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Testing dependencies
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    // Debug dependencies
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
