plugins {
    base
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.openapi.generator) apply false
    alias(libs.plugins.spotless)
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.dependency.analysis)
    id("khepri.module-graph")
}

spotless {
    val ktfmt = libs.versions.ktfmt.get()
    kotlin {
        target("build-logic/convention/src/**/*.kt")
        ktfmt(ktfmt).kotlinlangStyle().configure { it.setMaxWidth(120) }
    }
    kotlinGradle {
        target("*.gradle.kts", "build-logic/*.gradle.kts", "build-logic/convention/*.gradle.kts")
        ktfmt(ktfmt).kotlinlangStyle().configure { it.setMaxWidth(120) }
    }
}

dependencyAnalysis {
    issues {
        all {
            onAny { severity("fail") }
            onUsedTransitiveDependencies { severity("ignore") }
            onIncorrectConfiguration { severity("ignore") }
        }
    }
    structure { bundle("junit-jupiter") { includeGroup("org.junit.jupiter") } }
}

tasks.named("check") { dependsOn("assertModuleGraph", "buildHealth", "spotlessCheck") }
