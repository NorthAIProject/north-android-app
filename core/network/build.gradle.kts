plugins {
    id("khepri.core")
    id("khepri.hilt")
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:api"))
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)
}
