pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "khepri"

include(":app")

include(":core:model", ":core:common")

include(":detekt-rules")

project(":detekt-rules").projectDir = file("build-logic/detekt-rules")
