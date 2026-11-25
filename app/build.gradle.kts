plugins {
    id("khepri.android.application")
    id("khepri.compose")
    id("khepri.hilt")
    id("khepri.screenshot")
}

android {
    namespace = "com.fernandocorreia.khepri"
    defaultConfig {
        applicationId = "com.fernandocorreia.khepri"
        versionCode = 1
        versionName = "0.1.0"
    }
    buildFeatures { buildConfig = true }
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8090\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("String", "API_BASE_URL", "\"https://kheprios.com\"")
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:network"))
    implementation(project(":core:datastore"))
    implementation(project(":core:session"))
    implementation(libs.lifecycle.process)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.activity.compose)
    testImplementation(libs.hilt.android.testing)
}
