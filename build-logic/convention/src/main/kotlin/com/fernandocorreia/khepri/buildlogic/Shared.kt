package com.fernandocorreia.khepri.buildlogic

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.Lint
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

internal const val JVM_TARGET = 21
internal const val MIN_SDK = 28
internal const val TARGET_SDK = 37

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun Project.lib(alias: String): Provider<MinimalExternalModuleDependency> = libs.findLibrary(alias).get()

/** Compose 1.13 refuses to build against API 37.0, so every module compiles against 37.1. */
internal fun CommonExtension.configureAndroid() {
    compileSdk { version = release(TARGET_SDK) { minorApiLevel = 1 } }
    defaultConfig.minSdk = MIN_SDK
    testOptions.unitTests.isIncludeAndroidResources = true
}

/**
 * Lint warnings are errors, with no baseline. Test sources are skipped: lint 9.5 crashes analysing Kotlin 2.4 test
 * sources (FIR resolve error), and tests are not shipped.
 */
internal fun Lint.configureLint(project: Project) {
    warningsAsErrors = true
    abortOnError = true
    checkDependencies = false
    ignoreTestSources = true
    error += setOf("MissingTranslation", "ContentDescription", "UnusedResources")
    lintConfig = project.rootProject.file("config/lint/lint.xml")
}

internal fun Project.configureKotlin() {
    extensions.getByType<KotlinProjectExtension>().jvmToolchain(JVM_TARGET)
}

internal fun Project.androidCommon(): CommonExtension = extensions.getByType<CommonExtension>()
