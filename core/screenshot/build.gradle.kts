plugins {
    id("khepri.core")
    id("khepri.compose")
}

// Test support for screenshot tests: modules add it as testImplementation only.
dependencies {
    api(project(":core:designsystem"))
    api(libs.compose.ui.test.junit4)
    implementation(libs.roborazzi)
}
