plugins {
    id("khepri.core")
    id("khepri.compose")
    id("khepri.screenshot")
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:designsystem"))
}
