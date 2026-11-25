package com.fernandocorreia.khepri.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register

/**
 * `assertModuleGraph`: every project-to-project edge outside test configurations must match an allowed pattern
 * (`docs/design.md` 6.1), and no `feature:x:ui` may reach `data`, `:core:api`, `:core:network` or `:core:session`
 * through any path.
 */
class ModuleGraphPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        check(target == target.rootProject) { "khepri.module-graph is applied to the root project only" }
        val edges = target.provider {
            target.subprojects
                .flatMap { from ->
                    from.configurations
                        .filterNot { TEST_CONFIGURATION.containsMatchIn(it.name) }
                        .flatMap { c ->
                            c.dependencies.withType(ProjectDependency::class.java).map { "${from.path} -> ${it.path}" }
                        }
                }
                .filterNot { edge -> edge.split(" -> ").let { it[0] == it[1] } }
                .distinct()
                .sorted()
        }
        target.tasks.register<AssertModuleGraphTask>("assertModuleGraph") {
            group = "verification"
            description = "Checks module dependencies against docs/design.md 6.1."
            this.edges.set(edges)
        }
    }

    private companion object {
        val TEST_CONFIGURATION = Regex("(?i)test|detekt|lint")
    }
}

abstract class AssertModuleGraphTask : DefaultTask() {
    @get:Input abstract val edges: ListProperty<String>

    @TaskAction
    fun assertGraph() {
        val all = edges.get()
        val violations =
            all.filterNot { edge -> ALLOWED.any { it.matches(edge) } }.map { "not allowed: $it" }.toMutableList()
        val next = all.map { it.split(" -> ") }.groupBy({ it[0] }, { it[1] })
        next.keys
            .filter { UI.matches(it) }
            .forEach { ui ->
                val seen = mutableSetOf<String>()
                val queue = ArrayDeque(next[ui].orEmpty())
                while (queue.isNotEmpty()) {
                    val module = queue.removeFirst()
                    if (!seen.add(module)) continue
                    if (UI_NEVER_REACHES.matches(module)) violations += "$ui reaches $module"
                    queue.addAll(next[module].orEmpty())
                }
            }
        if (violations.isNotEmpty())
            throw GradleException("Module graph violates docs/design.md 6.1:\n" + violations.joinToString("\n"))
        logger.lifecycle("Module graph OK: ${all.size} edges")
    }

    private companion object {
        val UI = Regex(":feature:[a-z0-9]+:ui")
        val UI_NEVER_REACHES = Regex(":feature:[a-z0-9]+:data|:core:(api|network|session)")
        val ALLOWED =
            listOf(
                    ":app -> :(core|feature):.+",
                    ":feature:([a-z0-9]+):domain -> :core:(model|common)",
                    ":feature:([a-z0-9]+):data -> :feature:\\1:domain",
                    ":feature:([a-z0-9]+):data -> :core:(model|common|api|network|database|datastore)",
                    ":feature:([a-z0-9]+):ui -> :feature:\\1:domain",
                    ":feature:([a-z0-9]+):ui -> :core:(model|common|ui|designsystem|navigation|charts|scene3d|speech)",
                    ":core:common -> :core:model",
                    ":core:network -> :core:(model|common|api)",
                    ":core:(datastore|database|navigation) -> :core:(model|common)",
                    ":core:session -> :core:(model|common|api|network|datastore)",
                    ":core:designsystem -> :core:model",
                    ":core:ui -> :core:(model|common|designsystem)",
                    ":core:(charts|scene3d) -> :core:(designsystem|ui)",
                    ":core:(speech|health|notifications) -> :core:(model|common|api|network|datastore)",
                )
                .map(::Regex)
    }
}
