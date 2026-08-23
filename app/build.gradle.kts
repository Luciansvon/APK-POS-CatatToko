import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

val releaseStoreFile = providers.environmentVariable("CATATTOKO_RELEASE_STORE_FILE").orNull
val releaseStorePassword = providers.environmentVariable("CATATTOKO_RELEASE_STORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("CATATTOKO_RELEASE_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("CATATTOKO_RELEASE_KEY_PASSWORD").orNull
val releaseSigningValues = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
)
val releaseSigningConfigured = releaseSigningValues.all { !it.isNullOrBlank() }

if (!releaseSigningConfigured && releaseSigningValues.any { !it.isNullOrBlank() }) {
    throw GradleException("Konfigurasi signing release belum lengkap. Isi seluruh CATATTOKO_RELEASE_*.")
}

android {
    namespace = "com.bimacore.usahakecil"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.bimacore.usahakecil"
        minSdk = 23
        targetSdk = 36
        versionCode = 25
        versionName = "0.7.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    flavorDimensions += "business"
    productFlavors {
        create("retail") {
            dimension = "business"
            applicationIdSuffix = ".retail"
            versionNameSuffix = "-retail"
            buildConfigField("String", "BUSINESS_TYPE", "\"RETAIL\"")
        }
        create("wholesale") {
            dimension = "business"
            applicationIdSuffix = ".wholesale"
            versionNameSuffix = "-wholesale"
            buildConfigField("String", "BUSINESS_TYPE", "\"WHOLESALE\"")
        }
        create("culinary") {
            dimension = "business"
            applicationIdSuffix = ".culinary"
            versionNameSuffix = "-culinary"
            buildConfigField("String", "BUSINESS_TYPE", "\"CULINARY\"")
        }
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = file(requireNotNull(releaseStoreFile))
                storePassword = requireNotNull(releaseStorePassword)
                keyAlias = requireNotNull(releaseKeyAlias)
                keyPassword = requireNotNull(releaseKeyPassword)
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
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
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
}

tasks.register("verifyReleaseSigningReady") {
    group = "verification"
    description = "Fails unless production release signing is fully configured."
    doLast {
        check(releaseSigningConfigured) {
            "Release produksi diblokir: CATATTOKO_RELEASE_STORE_FILE, STORE_PASSWORD, KEY_ALIAS, dan KEY_PASSWORD wajib diisi."
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.06.00")
    val serializationBom =
        enforcedPlatform("org.jetbrains.kotlinx:kotlinx-serialization-bom:1.8.1")

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.12.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation(serializationBom)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")

    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.room:room-runtime:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    implementation("androidx.camera:camera-core:1.6.1")
    implementation("androidx.camera:camera-camera2:1.6.1")
    implementation("androidx.camera:camera-lifecycle:1.6.1")
    implementation("androidx.camera:camera-view:1.6.1")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")

    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:core:1.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation(serializationBom)
    androidTestImplementation("androidx.room:room-testing:2.8.4")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}
