package com.fernandocorreia.khepri.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

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
            dependencies {
                add("testImplementation", platform(lib("junit-bom")))
                add("testImplementation", lib("junit-jupiter"))
                add("testRuntimeOnly", lib("junit-platform-launcher"))
            }
            tasks.withType<Test>().configureEach { useJUnitPlatform() }
            limitTestTime()
        }
}
