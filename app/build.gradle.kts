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
        versionCode = 4
        versionName = "1.2.1"

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

// The privacy text names exactly what the app may do. INTERNET is there for handwriting only
// (Google ML Kit's model download and diagnostics, after the user agrees). Fail the build if a
// dependency adds any permission not in this list, so a new one can't slip into a release unnoticed.
val allowedPermissions = listOf(
    "android.permission.RECORD_AUDIO", // voice typing
    "android.permission.INTERNET", // handwriting (ML Kit)
    "android.permission.ACCESS_NETWORK_STATE", // ML Kit model download
    "android.permission.WAKE_LOCK", // WorkManager, used by ML Kit downloads
    "android.permission.RECEIVE_BOOT_COMPLETED", // WorkManager
    "android.permission.FOREGROUND_SERVICE", // WorkManager
    "DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION" // androidx.core, app-private
)

abstract class CheckPermissions : DefaultTask() {
    @get:Input
    abstract val allowed: ListProperty<String>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val mergedManifest: RegularFileProperty

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun verify() {
        val manifest = mergedManifest.get().asFile.readText()
        val declared = Regex("<uses-permission[^>]*android:name=\"([^\"]+)\"").findAll(manifest).map { it.groupValues[1] }.toList()
        val unexpected = declared.filter { name -> allowed.get().none { name == it || name.endsWith(".$it") } }
        check(unexpected.isEmpty()) {
            "The merged manifest declares permissions the privacy text doesn't cover: $unexpected. Find the " +
                "dependency that added them (manifest-merger report) and remove them, or update the in-app " +
                "Privacy page, privacy policy, Data safety and store listing, then add them to allowedPermissions."
        }
        report.get().asFile.writeText("OK: ${declared.joinToString()}\n")
    }
}

androidComponents {
    onVariants { variant ->
        val suffix = variant.name.replaceFirstChar(Char::uppercase)
        val checkTask = tasks.register<CheckPermissions>("check${suffix}Permissions") {
            allowed.set(allowedPermissions)
            mergedManifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
            report.set(layout.buildDirectory.file("reports/permissions/${variant.name}.txt"))
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

    // Handwriting
    implementation(libs.mlkit.digital.ink)

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
