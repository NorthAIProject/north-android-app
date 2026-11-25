package com.fernandocorreia.khepri.buildlogic

import io.github.takahirom.roborazzi.RoborazziExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Robolectric + Roborazzi screenshot tests. Record with `recordRoborazziDebug`, verify with `verifyRoborazziDebug`.
 * Reference images live in `src/test/screenshots` and are committed.
 */
class ScreenshotPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("io.github.takahirom.roborazzi")
            extensions.configure<RoborazziExtension> {
                outputDir.set(layout.projectDirectory.dir("src/test/screenshots"))
            }
            tasks.named("check") { dependsOn("verifyRoborazziDebug") }
            dependencies {
                add("testRuntimeOnly", lib("robolectric"))
                add("testRuntimeOnly", platform(lib("junit-bom")))
                add("testRuntimeOnly", lib("junit-vintage-engine"))
                add("testImplementation", lib("robolectric-annotations"))
                add("testImplementation", lib("roborazzi"))
                add("testImplementation", lib("roborazzi-compose"))
                add("testImplementation", lib("androidx-test-ext-junit"))
            }
        }
}
