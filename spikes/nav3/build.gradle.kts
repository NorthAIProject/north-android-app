plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.fernandocorreia.khepri.spike.nav3"
    compileSdk { version = release(37) { minorApiLevel = 1 } }
    defaultConfig {
        applicationId = "com.fernandocorreia.khepri.spike.nav3"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "s9"
    }
    buildFeatures { compose = true }
    buildTypes {
        release {
            // R8 full mode, as the product app will ship; signed with the debug key so the spike can be installed.
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

kotlin { jvmToolchain(21) }

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)
    implementation(libs.lifecycle.viewmodel.navigation3)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.kotlinx.serialization.json)
}
