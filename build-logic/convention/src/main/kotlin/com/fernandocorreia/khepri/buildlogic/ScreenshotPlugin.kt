package com.fernandocorreia.khepri.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import io.github.takahirom.roborazzi.RoborazziExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Robolectric + Roborazzi screenshot tests. Record with `recordRoborazziDebug`, verify with `verifyRoborazziDebug`.
 * Reference images live in `src/test/screenshots` and are committed.
 */
class ScreenshotPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("io.github.takahirom.roborazzi")
        extensions.configure<RoborazziExtension> { outputDir.set(layout.projectDirectory.dir("src/test/screenshots")) }
        dependencies {
            add("testImplementation", lib("robolectric"))
            add("testImplementation", lib("roborazzi"))
            add("testImplementation", lib("roborazzi-compose"))
            add("testImplementation", lib("roborazzi-junit-rule"))
            add("testImplementation", lib("androidx-test-ext-junit"))
        }
    }
}
