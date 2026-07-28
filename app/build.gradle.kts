import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

/* Read the openconnect build settings out of gradle.properties so the shell
 * script and the Gradle build never disagree about version / ABI list. */
val ocVersion: String = providers.gradleProperty("openconnect.version").getOrElse("9.21")
val ocAbis: List<String> = providers.gradleProperty("openconnect.abis")
    .getOrElse("arm64-v8a,armeabi-v7a,x86_64")
    .split(",").map { it.trim() }.filter { it.isNotEmpty() }
val ocMinApi: String = providers.gradleProperty("openconnect.minApi").getOrElse("26")

android {
    namespace = "dev.opentunnel.vpn"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.opentunnel.vpn"
        minSdk = 26
        targetSdk = 35
        versionCode = 11
        versionName = System.getenv("GITHUB_REF_NAME")?.removePrefix("v")?.takeIf { it.isNotBlank() } ?: "3.3.0"

        ndk {
            // Only package the ABIs the native script actually produced.
            abiFilters += ocAbis
        }

        buildConfigField("String", "OPENCONNECT_VERSION", "\"$ocVersion\"")
    }

    applicationVariants.all {
        val buildType = name
        outputs.all {
            val output = this as? com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output?.outputFileName = "opentunnel_${versionName}_${buildType}.apk"
        }
    }

    signingConfigs {
        val keystorePropsFile = rootProject.file("keystore.properties")
        val envStoreFile = System.getenv("KEYSTORE_FILE")
        val envStorePassword = System.getenv("KEYSTORE_PASSWORD")
        val envKeyAlias = System.getenv("KEY_ALIAS")
        val envKeyPassword = System.getenv("KEY_PASSWORD")

        create("release") {
            val repoKeystore = rootProject.file("release.keystore")
            if (keystorePropsFile.exists()) {
                val props = Properties().apply { keystorePropsFile.inputStream().use { load(it) } }
                storeFile = rootProject.file(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            } else if (!envStoreFile.isNullOrBlank() && !envStorePassword.isNullOrBlank()) {
                storeFile = file(envStoreFile)
                storePassword = envStorePassword
                keyAlias = envKeyAlias ?: ""
                keyPassword = envKeyPassword ?: envStorePassword
            } else if (repoKeystore.exists() && repoKeystore.length() > 0L) {
                storeFile = repoKeystore
                storePassword = System.getenv("KEYSTORE_PASSWORD")?.takeIf { it.isNotBlank() } ?: "android"
                keyAlias = System.getenv("KEY_ALIAS")?.takeIf { it.isNotBlank() } ?: "androiddebugkey"
                keyPassword = System.getenv("KEY_PASSWORD")?.takeIf { it.isNotBlank() } ?: "android"
            } else {
                // Fall back to debug signing config for local developer builds when no release credentials exist
                val debugConfig = signingConfigs.getByName("debug")
                storeFile = debugConfig.storeFile
                storePassword = debugConfig.storePassword
                keyAlias = debugConfig.keyAlias
                keyPassword = debugConfig.keyPassword
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-opt-in=kotlin.RequiresOptIn")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "/META-INF/DEPENDENCIES",
        )
        jniLibs {
            // Load libopenconnect.so straight out of the APK (no extraction).
            useLegacyPackaging = false
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)

    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
}

/* ---------------------------------------------------------------------------
 * Native library plumbing
 * ------------------------------------------------------------------------ */

val jniLibsDir = layout.projectDirectory.dir("src/main/jniLibs")

/**
 * Cross-compiles openconnect + OpenSSL + libxml2 + lz4 for every ABI listed in
 * gradle.properties and drops the resulting libopenconnect.so into jniLibs.
 *
 * Requires a Linux/macOS shell with autotools and ANDROID_NDK_HOME set.
 * Run once: ./gradlew :app:buildNativeLibs   (or ./native/build-openconnect.sh)
 */
tasks.register<Exec>("buildNativeLibs") {
    group = "build"
    description = "Cross-compiles openconnect $ocVersion into app/src/main/jniLibs"
    workingDir = rootProject.projectDir
    commandLine(
        "bash", "native/build-openconnect.sh",
        "--version", ocVersion,
        "--abis", ocAbis.joinToString(","),
        "--api", ocMinApi,
        "--out", jniLibsDir.asFile.absolutePath,
        "--java-out", layout.projectDirectory.dir("src/main/java").asFile.absolutePath,
    )
}

/** Non-fatal heads-up when someone builds the APK before the native step. */
val checkNativeLibs = tasks.register("checkNativeLibs") {
    group = "verification"
    description = "Warns when libopenconnect.so has not been built yet"
    val abis = ocAbis
    val dir = jniLibsDir.asFile
    doLast {
        val missing = abis.filter { !File(dir, "$it/libopenconnect.so").exists() }
        if (missing.isNotEmpty()) {
            logger.warn(
                """
                |
                |  ┌──────────────────────────────────────────────────────────────────┐
                |  │  libopenconnect.so is missing for: ${missing.joinToString(", ").padEnd(29)}│
                |  │                                                                  │
                |  │  The app will install and the UI will run, but connecting will   │
                |  │  fail with "native library not available".                       │
                |  │                                                                  │
                |  │  Build it with:   ./gradlew :app:buildNativeLibs                 │
                |  │  (needs ANDROID_NDK_HOME + autotools; see native/README.md)      │
                |  └──────────────────────────────────────────────────────────────────┘
                |
                """.trimMargin()
            )
        }
    }
}

tasks.named("preBuild") { dependsOn(checkNativeLibs) }
