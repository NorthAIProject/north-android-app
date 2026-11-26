plugins { id("khepri.feature.data") }

dependencies {
    implementation(project(":core:common"))
    testImplementation(libs.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlinx.serialization.json)
}
