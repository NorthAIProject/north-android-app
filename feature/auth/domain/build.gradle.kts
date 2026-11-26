plugins { id("khepri.feature.domain") }

dependencies {
    implementation(libs.javax.inject)
    testImplementation(libs.kotlinx.coroutines.test)
}
