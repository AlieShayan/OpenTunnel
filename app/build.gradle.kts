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
        versionCode = 12
        versionName = System.getenv("GITHUB_REF_NAME")?.removePrefix("v")?.takeIf { it.isNotBlank() } ?: "3.4.0"

        ndk {
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
        // ── helpers ──────────────────────────────────────────────────────────
        // Priority order for release keystore resolution:
        //   1. keystore.properties file in the root project (local developer machine)
        //   2. Environment variables (CI: KEYSTORE_FILE, KEYSTORE_PASSWORD, KEY_ALIAS)
        //   3. release.keystore file in the root project + env passwords (CI fallback)
        //
        // If NONE of the above are available the release signingConfig is left
        // intentionally INVALID so that `assembleRelease` fails loudly instead
        // of silently producing a debug-signed release APK that would cause
        // "App not installed as package conflicts with an existing package"
        // when a user tries to update over a properly-signed build.

        val keystorePropsFile = rootProject.file("keystore.properties")
        val envStoreFile      = System.getenv("KEYSTORE_FILE")?.takeIf { it.isNotBlank() }
        val envStorePassword  = System.getenv("KEYSTORE_PASSWORD")?.takeIf { it.isNotBlank() }
        val envKeyAlias       = System.getenv("KEY_ALIAS")?.takeIf { it.isNotBlank() }
        val envKeyPassword    = System.getenv("KEY_PASSWORD")?.takeIf { it.isNotBlank() } ?: envStorePassword
        val repoKeystore      = rootProject.file("release.keystore")

        create("release") {
            when {
                // 1. Local keystore.properties
                keystorePropsFile.exists() -> {
                    val props = Properties().apply { keystorePropsFile.inputStream().use { load(it) } }
                    storeFile     = rootProject.file(props.getProperty("storeFile"))
                    storePassword = props.getProperty("storePassword")
                    keyAlias      = props.getProperty("keyAlias")
                    keyPassword   = props.getProperty("keyPassword")
                    println("[Signing] Using keystore.properties")
                }

                // 2. Env KEYSTORE_FILE
                envStoreFile != null && envStorePassword != null && envKeyAlias != null -> {
                    val envFile = rootProject.file(envStoreFile)
                    if (envFile.exists() && envFile.length() > 0L) {
                        storeFile     = envFile
                        storePassword = envStorePassword
                        keyAlias      = envKeyAlias
                        keyPassword   = envKeyPassword
                        println("[Signing] Using KEYSTORE_FILE env variable")
                    } else {
                        throw GradleException(
                            "[Signing] KEYSTORE_FILE '${envFile.absolutePath}' does not exist or is empty. " +
                            "Cannot produce a consistently-signed release APK."
                        )
                    }
                }

                // 3. release.keystore in repo root + env passwords
                repoKeystore.exists() && repoKeystore.length() > 0L &&
                envStorePassword != null && envKeyAlias != null -> {
                    storeFile     = repoKeystore
                    storePassword = envStorePassword
                    keyAlias      = envKeyAlias
                    keyPassword   = envKeyPassword
                    println("[Signing] Using release.keystore from repo root")
                }

                // No credentials at all — fail loudly for release builds.
                else -> {
                    // Setting storeFile to null causes Gradle to throw during
                    // assembleRelease; assembleDebug is unaffected.
                    println(
                        "[Signing] WARNING: No release signing credentials found. " +
                        "assembleRelease WILL FAIL. Set up keystore.properties or GitHub Secrets."
                    )
                    storeFile = null
                }
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            // Always uses the auto-generated debug keystore — never touches release signingConfig.
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
                |  ┌────────────────────────────────────────────────────────────────────┐
                |  │  libopenconnect.so is missing for: ${missing.joinToString(", ").padEnd(29)}│
                |  │                                                                  │
                |  │  The app will install and the UI will run, but connecting will   │
                |  │  fail with "native library not available".                       │
                |  │                                                                  │
                |  │  Build it with:   ./gradlew :app:buildNativeLibs                 │
                |  │  (needs ANDROID_NDK_HOME + autotools; see native/README.md)      │
                |  └────────────────────────────────────────────────────────────────────┘
                |
                """.trimMargin()
            )
        }
    }
}

tasks.named("preBuild") { dependsOn(checkNativeLibs) }
