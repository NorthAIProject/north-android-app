package com.fernandocorreia.khepri.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

class KotlinJvmPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            pluginManager.apply("khepri.quality")
            configureKotlin()
            dependencies {
                add("testImplementation", platform(lib("junit-bom")))
                add("testImplementation", lib("junit-jupiter"))
                add("testRuntimeOnly", lib("junit-platform-launcher"))
            }
            tasks.withType<Test>().configureEach { useJUnitPlatform() }
            limitTestTime()
        }
}
