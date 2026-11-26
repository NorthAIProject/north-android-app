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

include(
    ":core:model",
    ":core:common",
    ":core:api",
    ":core:network",
    ":core:datastore",
    ":core:database",
    ":core:session",
)

include(":core:designsystem", ":core:ui", ":core:testing", ":core:navigation")

include(":feature:auth:domain", ":feature:auth:data", ":feature:auth:ui")

include(":feature:today:domain", ":feature:today:data")

include(":detekt-rules")

project(":detekt-rules").projectDir = file("build-logic/detekt-rules")
