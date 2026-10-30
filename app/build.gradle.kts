plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val releaseSigningVariables = listOf(
    "RUMB_KEYSTORE_PATH",
    "RUMB_KEYSTORE_PASSWORD",
    "RUMB_KEY_ALIAS",
    "RUMB_KEY_PASSWORD",
)
val releaseSigningValues = releaseSigningVariables.associateWith { name ->
    providers.environmentVariable(name).orNull?.takeIf(String::isNotBlank)
}
val configuredSigningVariables = releaseSigningValues.filterValues { it != null }.keys
if (configuredSigningVariables.isNotEmpty() && configuredSigningVariables.size != releaseSigningVariables.size) {
    val missing = releaseSigningVariables.filterNot(configuredSigningVariables::contains)
    throw GradleException(
        "Incomplete release signing configuration. Set all release signing environment variables; missing: ${missing.joinToString()}.",
    )
}

android {
    namespace = "com.rumb.launcher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rumb.launcher.next"
        minSdk = 26
        targetSdk = 35
        versionCode = 7
        versionName = "3.0.0"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }

    val releaseSigningConfig = if (configuredSigningVariables.size == releaseSigningVariables.size) {
        signingConfigs.create("release") {
            storeFile = file(releaseSigningValues.getValue("RUMB_KEYSTORE_PATH")!!)
            storePassword = releaseSigningValues.getValue("RUMB_KEYSTORE_PASSWORD")!!
            keyAlias = releaseSigningValues.getValue("RUMB_KEY_ALIAS")!!
            keyPassword = releaseSigningValues.getValue("RUMB_KEY_PASSWORD")!!
        }
    } else {
        null
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            releaseSigningConfig?.let { signingConfig = it }
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
