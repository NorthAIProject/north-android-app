plugins {
    id("khepri.core")
    id("khepri.compose")
    id("khepri.screenshot")
}

dependencies { implementation(libs.compose.ui.tooling.preview) }
