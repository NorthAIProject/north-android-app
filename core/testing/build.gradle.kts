plugins { id("khepri.core.jvm") }

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.test)
    implementation(platform(libs.junit.bom))
    implementation(libs.junit.jupiter.api)
}
