package com.fernandocorreia.khepri.buildlogic

import com.android.build.api.dsl.CommonExtension
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

internal fun Project.configureKotlin() {
    extensions.getByType<KotlinProjectExtension>().jvmToolchain(JVM_TARGET)
}

internal fun Project.androidCommon(): CommonExtension = extensions.getByType<CommonExtension>()
