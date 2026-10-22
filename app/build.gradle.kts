@file:Suppress("UnstableApiUsage")

import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
}

val localProperties = Properties()
localProperties.apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

val signingPropertiesFile = rootProject.file("signing.properties")
val signingProperties = Properties()
if (signingPropertiesFile.exists()) {
    signingPropertiesFile.inputStream().use { signingProperties.load(it) }
}
fun findSigningProperty(key: String): String? {
    return signingProperties.getProperty(key)
        ?: (project.findProperty(key) as? String)
        ?: System.getenv(key)
        ?: System.getenv(key.replace(Regex("([a-z])([A-Z])"), "$1_$2").uppercase())
}

val releaseStoreFileProp = findSigningProperty("releaseStoreFile")
val releaseStorePasswordProp = findSigningProperty("releaseStorePassword")
val releaseKeyAliasProp = findSigningProperty("releaseKeyAlias")
val releaseKeyPasswordProp = findSigningProperty("releaseKeyPassword")

val releaseStoreFile = releaseStoreFileProp?.let { rootProject.file(it) }
val hasReleaseSigning = releaseStoreFile?.exists() == true &&
    !releaseStorePasswordProp.isNullOrBlank() &&
    !releaseKeyAliasProp.isNullOrBlank() &&
    !releaseKeyPasswordProp.isNullOrBlank()

if (!hasReleaseSigning) {
    logger.warn(
        "release 构建未配置有效签名私钥，产出 APK 将不可直接安装（报 INSTALL_PARSE_FAILED_NO_CERTIFICATES）。" +
            "本地如需签署 release 包，请参考 signing.properties.template 配置 signing.properties 或注入环境变量。"
    )
}

android {
    namespace = AppConfiguration.appId
    compileSdk = AppConfiguration.compileSdk

    signingConfigs {
        getByName("debug") {
            enableV1Signing = true
            enableV2Signing = true
        }
        if (hasReleaseSigning && releaseStoreFile != null) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = releaseStorePasswordProp
                keyAlias = releaseKeyAliasProp
                keyPassword = releaseKeyPasswordProp
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    defaultConfig {
        applicationId = AppConfiguration.applicationId
        minSdk = AppConfiguration.minSdk
        targetSdk = AppConfiguration.targetSdk
        versionCode = AppConfiguration.versionCode
        versionName = AppConfiguration.versionName

        testInstrumentationRunner = "dev.frost819.newbv.app.CustomTestRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField(
            "String",
            "CRASH_REPORT_TOKEN",
            "\"${localProperties.getProperty("crashReport.token", "")}\"",
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfigs.findByName("release")?.let { releaseSigning ->
                signingConfig = releaseSigning
            }
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }

    androidComponents {
        onVariants { variant ->
            variant.outputs.forEach { output ->
                if (output is com.android.build.api.variant.impl.VariantOutputImpl) {
                    output.outputFileName.set(
                        "BTV_${AppConfiguration.versionCode}_${AppConfiguration.versionName}_${variant.name}.apk",
                    )
                }
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            // Protobuf source files and desktop-only Jansi natives are not used at runtime on Android.
            excludes +=
                listOf(
                    "**/*.proto",
                    "/META-INF/native-image/jansi/**",
                    "/org/fusesource/jansi/internal/native/**",
                )
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // === Project modules ===
    implementation(project(":bili-api"))
    implementation(project(":bili-subtitle"))
    implementation(project(":player"))
    implementation(project(":danmaku"))
    implementation(project(":core"))
    implementation(project(":data"))

    // === AndroidX Core ===
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.material)

    // === Lifecycle ===
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // === Navigation ===
    implementation(libs.androidx.navigation.compose)

    // === DataStore ===
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.datastore.preferences)

    // === Room ===
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // === Compose ===
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.util)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.compose.tv.foundation)
    implementation(libs.androidx.compose.tv.material)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // === Hilt ===
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // === Media3 ===
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.datasource.okhttp)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.exoplayer.hls)

    // === Image loading (Coil 3) ===
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // === Networking ===
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.encoding)
    implementation(libs.ktor.client.serialization.kotlinx)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)

    // === Kotlin ===
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.serialization)

    // === Other ===
    implementation(project(":danmaku-engine"))
    implementation(libs.androidsvg)
    implementation(libs.qrcode)
    debugImplementation(libs.leakcanary)

    // === Testing ===
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.kotlin.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)




    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.compiler)

    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.androidx.room.testing)
}

tasks.withType<Test> {
    useJUnitPlatform()
    maxHeapSize = "2048m"
    forkEvery = 50L
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
}
