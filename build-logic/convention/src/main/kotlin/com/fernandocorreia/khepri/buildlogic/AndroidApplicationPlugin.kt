package com.fernandocorreia.khepri.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("khepri.quality")
            extensions.configure<ApplicationExtension> {
                configureAndroid()
                lint { configureLint(target) }
                defaultConfig.targetSdk = TARGET_SDK
            }
            configureKotlin()
        }
}
