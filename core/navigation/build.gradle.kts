plugins {
    id("khepri.core")
    id("khepri.compose")
    id("khepri.hilt")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(project(":core:common"))
    api(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.navigation3)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(project(":core:testing"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.test.ext.junit)
    testRuntimeOnly(libs.robolectric)
    testRuntimeOnly(libs.junit.vintage.engine)
}
