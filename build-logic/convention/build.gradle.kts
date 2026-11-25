plugins { `kotlin-dsl` }

group = "com.fernandocorreia.khepri.buildlogic"

kotlin { jvmToolchain(21) }

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.compose.compiler.gradle.plugin)
    compileOnly(libs.ksp.gradle.plugin)
    compileOnly(libs.hilt.gradle.plugin)
    compileOnly(libs.roborazzi.gradle.plugin)
    compileOnly(libs.spotless.gradle.plugin)
    compileOnly(libs.detekt.gradle.plugin)
}

gradlePlugin {
    plugins {
        fun add(id: String, impl: String) =
            register(id) {
                this.id = id
                implementationClass = "com.fernandocorreia.khepri.buildlogic.$impl"
            }
        add("khepri.android.application", "AndroidApplicationPlugin")
        add("khepri.android.library", "AndroidLibraryPlugin")
        add("khepri.kotlin.jvm", "KotlinJvmPlugin")
        add("khepri.compose", "ComposePlugin")
        add("khepri.hilt", "HiltPlugin")
        add("khepri.screenshot", "ScreenshotPlugin")
        add("khepri.core", "CorePlugin")
        add("khepri.feature.domain", "FeatureDomainPlugin")
        add("khepri.feature.data", "FeatureDataPlugin")
        add("khepri.feature.ui", "FeatureUiPlugin")
        add("khepri.quality", "QualityPlugin")
        add("khepri.module-graph", "ModuleGraphPlugin")
    }
}
