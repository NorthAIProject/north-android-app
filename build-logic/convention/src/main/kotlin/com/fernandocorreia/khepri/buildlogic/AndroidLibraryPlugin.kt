package com.fernandocorreia.khepri.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("com.android.library")
            pluginManager.apply("khepri.quality")
            extensions.configure<LibraryExtension> {
                configureAndroid()
                lint { configureLint(target) }
                namespace = "com.fernandocorreia.khepri" + path.replace(':', '.')
            }
            configureKotlin()
        }
}
