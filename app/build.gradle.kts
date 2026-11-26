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
        // Release code (R8, resource shrinking) against the local backend, signed with the debug key: for the smoke
        // flow and R8 checks. Never uploaded. It copies `release`, so it must come after it.
        create("minified") {
            initWith(getByName("release"))
            applicationIdSuffix = ".minified"
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8090\"")
        }
    }
}

/** Release reaches only kheprios.com: what lets local builds reach a dev machine must never merge into it. */
abstract class VerifyReleaseManifest : DefaultTask() {
    @get:InputFile @get:PathSensitive(PathSensitivity.NONE) abstract val manifest: RegularFileProperty

    @TaskAction
    fun verify() {
        val text = manifest.get().asFile.readText()
        check("android.permission.ACCESS_LOCAL_NETWORK" !in text) { "release must not ask for ACCESS_LOCAL_NETWORK" }
        check("usesCleartextTraffic=\"true\"" !in text) { "release must not allow cleartext" }
    }
}

androidComponents {
    // A build type that copies `release` before `release` is configured gets no R8 and still builds. Fail instead.
    onVariants(selector().withBuildType("release")) { variant ->
        check(variant.isMinifyEnabled) { "release must run R8" }
        val verify =
            tasks.register<VerifyReleaseManifest>("verifyReleaseManifest") {
                manifest.set(variant.artifacts.get(com.android.build.api.artifact.SingleArtifact.MERGED_MANIFEST))
            }
        tasks.named("check") { dependsOn(verify) }
    }
    onVariants(selector().withBuildType("minified")) { check(it.isMinifyEnabled) { "minified must run R8" } }
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
