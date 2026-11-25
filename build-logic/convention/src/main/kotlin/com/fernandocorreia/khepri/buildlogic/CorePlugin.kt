package com.fernandocorreia.khepri.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

/**
 * An Android library under `core/` with explicit API mode. `:core:api` and `:core:testing` use
 * `khepri.android.library`.
 */
class CorePlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("khepri.android.library")
            extensions.getByType<KotlinProjectExtension>().explicitApi()
        }
}
