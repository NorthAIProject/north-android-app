package com.fernandocorreia.khepri.buildlogic

import com.diffplug.gradle.spotless.SpotlessExtension
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Spotless (ktfmt, width 120) and detekt with the Compose rules and our own rule set. Applied by every module plugin.
 */
class QualityPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("com.diffplug.spotless")
            pluginManager.apply("dev.detekt")
            val ktfmt = libs.findVersion("ktfmt").get().requiredVersion
            extensions.configure<SpotlessExtension> {
                kotlin {
                    target("src/**/*.kt")
                    targetExclude("**/build/**")
                    ktfmt(ktfmt).kotlinlangStyle().configure { it.setMaxWidth(MAX_WIDTH) }
                }
                kotlinGradle {
                    target("*.gradle.kts")
                    ktfmt(ktfmt).kotlinlangStyle().configure { it.setMaxWidth(MAX_WIDTH) }
                }
            }
            extensions.configure<DetektExtension> {
                buildUponDefaultConfig.set(true)
                parallel.set(true)
                config.setFrom(rootProject.file("config/detekt/detekt.yml"))
            }
            dependencies {
                add("detektPlugins", lib("compose-rules-detekt"))
                if (path != ":detekt-rules") add("detektPlugins", project(":detekt-rules"))
            }
        }

    private companion object {
        const val MAX_WIDTH = 120
    }
}
