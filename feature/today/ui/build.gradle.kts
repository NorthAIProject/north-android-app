plugins { id("khepri.feature.ui") }

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:navigation"))
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)

    testImplementation(project(":core:testing"))
    testImplementation(project(":core:screenshot"))
    testImplementation(libs.kotlinx.coroutines.test)
}
