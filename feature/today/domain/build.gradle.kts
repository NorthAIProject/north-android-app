plugins { id("khepri.feature.domain") }

dependencies {
    implementation(libs.javax.inject)
    api(libs.kotlinx.coroutines.core)
    testImplementation(project(":core:testing"))
    testImplementation(libs.kotlinx.coroutines.test)
}
