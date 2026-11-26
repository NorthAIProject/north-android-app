plugins { id("khepri.feature.domain") }

dependencies {
    implementation(libs.javax.inject)
    api(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
