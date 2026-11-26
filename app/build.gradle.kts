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
        // Release code (R8, resource shrinking) against the local backend, signed with the debug key: for the smoke
        // flow and R8 checks. Never uploaded.
        create("minified") {
            initWith(getByName("release"))
            applicationIdSuffix = ".minified"
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
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
    implementation(project(":core:navigation"))
    implementation(libs.datastore.preferences)
    implementation(project(":core:session"))
    implementation(project(":core:model"))
    implementation(project(":core:ui"))
    implementation(project(":core:designsystem"))
    implementation(project(":feature:auth:ui"))
    implementation(project(":feature:auth:data"))
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.process)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.activity.compose)
    implementation(libs.compose.ui.tooling.preview)
    testImplementation(libs.hilt.android.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(project(":feature:auth:domain"))
    testImplementation(libs.okhttp)
}
