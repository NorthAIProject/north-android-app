plugins {
    id("khepri.core")
    id("khepri.compose")
    id("khepri.screenshot")
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:designsystem"))
}
