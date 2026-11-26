plugins {
    id("khepri.core")
    id("khepri.hilt")
    id("khepri.room")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(project(":core:common"))
    implementation(libs.sqlite)
    implementation(libs.sqlite.framework)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.test.ext.junit)
    testRuntimeOnly(libs.robolectric)
    testRuntimeOnly(libs.junit.vintage.engine)
}
