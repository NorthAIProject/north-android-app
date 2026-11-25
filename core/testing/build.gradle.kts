plugins { id("khepri.core.jvm") }

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.coroutines.test)
    api(platform(libs.junit.bom))
    api(libs.junit.jupiter.api)
}
