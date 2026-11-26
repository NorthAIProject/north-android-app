package com.fernandocorreia.khepri.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

private fun Project.sibling(name: String): String = path.substringBeforeLast(':') + ":$name"

/** `feature:x:domain`: pure Kotlin; sees only `:core:model` and `:core:common`. */
class FeatureDomainPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("khepri.kotlin.jvm")
            dependencies {
                add("implementation", project(":core:model"))
                add("implementation", project(":core:common"))
            }
        }
}

/** `feature:x:data`. Add `:core:common`, `:core:database` or `:core:datastore` per module, as used. */
class FeatureDataPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("khepri.android.library")
            pluginManager.apply("khepri.hilt")
            dependencies {
                add("implementation", project(sibling("domain")))
                listOf(":core:model", ":core:api", ":core:network").forEach {
                    add("implementation", project(it))
                }
                // The generated interfaces return retrofit2.Response, which apiCall takes.
                add("implementation", lib("retrofit"))
            }
        }
}

/** `feature:x:ui`. Never sees `feature:x:data` or `:core:api`. Add `:core:navigation` per module, as used. */
class FeatureUiPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("khepri.android.library")
            pluginManager.apply("khepri.compose")
            pluginManager.apply("khepri.hilt")
            pluginManager.apply("khepri.screenshot")
            dependencies {
                add("implementation", project(sibling("domain")))
                listOf(":core:ui", ":core:designsystem").forEach {
                    add("implementation", project(it))
                }
            }
        }
}
