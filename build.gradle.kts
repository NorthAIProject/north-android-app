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
            onUnusedDependencies { exclude("com.google.dagger:hilt-android") }
        }
    }
    structure {
        bundle("junit-jupiter") { includeGroup("org.junit.jupiter") }
        bundle("roborazzi") { includeGroup("io.github.takahirom.roborazzi") }
    }
}

val checkTranslations =
    tasks.register<Exec>("checkTranslations") {
        description = "Fails when a string is missing from, or extra in, any locale of any module."
        group = "verification"
        commandLine("python3", "tools/check_translations.py")
    }

val testStringTools =
    tasks.register<Exec>("testStringTools") {
        description = "Runs the tests of the string and theme tools in tools/."
        group = "verification"
        commandLine("python3", "-m", "unittest", "discover", "-s", "tools", "-p", "test_*.py")
    }

tasks.named("check") {
    dependsOn("assertModuleGraph", "buildHealth", "spotlessCheck", checkTranslations, testStringTools)
}
