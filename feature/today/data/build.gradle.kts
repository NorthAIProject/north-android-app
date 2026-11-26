plugins { id("khepri.feature.data") }

android {
    // The contract's golden responses are the test fixtures.
    sourceSets.getByName("test").resources.srcDir(rootProject.file("core/api/contract/golden"))
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:database"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.room3.runtime)
    testImplementation(libs.sqlite.framework)
    testRuntimeOnly(libs.robolectric)
    testRuntimeOnly(libs.junit.vintage.engine)
}
