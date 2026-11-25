package com.fernandocorreia.khepri.buildlogic

import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.process.ExecOperations

/**
 * Copies the API contract from `north-client` at the pinned commit into `contract/`: the spec, the medals file and every
 * `internal/<package>/testdata/<name>.golden.json` (as `golden/<name>.golden.json`; `profiles` goldens get a `profiles-`
 * prefix because `profile.golden.json` exists twice). Reads git objects with `git show`, never the working tree. With
 * `--check` it writes nothing and fails if any committed copy differs, is missing or is stale.
 */
abstract class SyncContractTask @Inject constructor(private val exec: ExecOperations) : DefaultTask() {
    @get:Input abstract val sha: Property<String>

    @get:Internal abstract val clientRepo: DirectoryProperty

    @get:Internal abstract val contractDir: DirectoryProperty

    @get:Input
    @get:Option(option = "check", description = "Fail if the committed contract differs from the pinned commit.")
    abstract val checkOnly: Property<Boolean>

    init {
        checkOnly.convention(false)
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun sync() {
        val commit = sha.get()
        val wanted =
            git("ls-tree", "-r", "--name-only", commit)
                .decodeToString()
                .lines()
                .mapNotNull { path -> destination(path)?.let { it to path } }
                .toMap()
        if (wanted.size != wanted.values.toSet().size) throw GradleException("syncContract: two sources map to one file")
        val root = contractDir.get().asFile
        val present = root.walkTopDown().filter { it.isFile }.map { it.relativeTo(root).invariantSeparatorsPath }.toSet()
        val stale = present - wanted.keys
        val problems = mutableListOf<String>()
        wanted.forEach { (dest, source) ->
            val bytes = git("show", "$commit:$source")
            val file = File(root, dest)
            if (checkOnly.get()) {
                if (!file.exists()) problems += "missing $dest" else if (!file.readBytes().contentEquals(bytes)) problems += "differs $dest"
            } else {
                file.parentFile.mkdirs()
                file.writeBytes(bytes)
            }
        }
        if (checkOnly.get()) {
            problems += stale.map { "stale $it" }
            if (problems.isNotEmpty()) throw GradleException("Contract is not in sync with $commit:\n" + problems.joinToString("\n"))
            logger.lifecycle("Contract matches $commit (${wanted.size} files)")
        } else {
            stale.forEach { File(root, it).delete() }
            logger.lifecycle("Contract synced from $commit: ${wanted.size} files, ${stale.size} stale removed")
        }
    }

    private fun destination(path: String): String? =
        when {
            path == "docs/api/openapi.yaml" -> "openapi.yaml"
            path == "docs/medals/medals.json" -> "medals.json"
            else ->
                GOLDEN.matchEntire(path)?.destructured?.let { (pkg, name) ->
                    "golden/" + (if (pkg == "profiles") "profiles-" else "") + "$name.golden.json"
                }
        }

    private fun git(vararg args: String): ByteArray {
        val out = ByteArrayOutputStream()
        exec.exec {
            commandLine("git", "-C", clientRepo.get().asFile.absolutePath, *args)
            standardOutput = out
        }
        return out.toByteArray()
    }

    private companion object {
        val GOLDEN = Regex("internal/([^/]+)/testdata/([^/]+)\\.golden\\.json")
    }
}
