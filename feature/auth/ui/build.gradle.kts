plugins { id("khepri.feature.ui") }

dependencies {
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(project(":core:common"))
    testImplementation(project(":core:testing"))
    testImplementation(project(":core:screenshot"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
