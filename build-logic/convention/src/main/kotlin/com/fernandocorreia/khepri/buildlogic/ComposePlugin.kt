package com.fernandocorreia.khepri.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class ComposePlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        androidCommon().buildFeatures.compose = true
        dependencies {
            val bom = platform(lib("compose-bom"))
            add("implementation", bom)
            add("testImplementation", bom)
            add("implementation", lib("compose-material3"))
            add("implementation", lib("compose-ui-tooling-preview"))
            add("debugImplementation", lib("compose-ui-tooling"))
            add("debugImplementation", lib("compose-ui-test-manifest"))
            add("testImplementation", lib("compose-ui-test-junit4"))
        }
    }
}
