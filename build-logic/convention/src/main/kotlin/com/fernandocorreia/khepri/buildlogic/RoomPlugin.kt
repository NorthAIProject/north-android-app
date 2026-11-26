package com.fernandocorreia.khepri.buildlogic

import androidx.room3.gradle.RoomExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** Room 3 with KSP. Schemas are exported to `schemas/` and committed, so every migration has its before and after. */
class RoomPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("com.google.devtools.ksp")
            pluginManager.apply("androidx.room3")
            extensions.configure<RoomExtension> { schemaDirectory(layout.projectDirectory.dir("schemas")) }
            dependencies {
                add("implementation", lib("room3-runtime"))
                add("ksp", lib("room3-compiler"))
            }
        }
}
