import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fernandocorreia.khepri.buildlogic.SyncContractTask
import dev.detekt.gradle.Detekt
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    id("khepri.kotlin.jvm")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.openapi.generator)
}

val contractDir = layout.projectDirectory.dir("contract")
val normalizedSpec = layout.buildDirectory.file("normalized/openapi.yaml")
val operationsList = layout.buildDirectory.file("normalized/operations.tsv")
val generatedDir = layout.buildDirectory.dir("generated/openapi")
val operationIndexDir = layout.buildDirectory.dir("generated/operationIndex")

tasks.register<SyncContractTask>("syncContract") {
    group = "contract"
    description = "Copies the spec, medals and goldens from north-client at contract.sha into contract/."
    sha.set(providers.fileContents(layout.projectDirectory.file("contract.sha")).asText.map { it.trim() })
    clientRepo.set(rootProject.layout.projectDirectory.dir("../north-client"))
    contractDir.set(layout.projectDirectory.dir("contract"))
}

/**
 * Operations the generator must not see: event streams and multipart bodies are hand-written. These three stream
 * without declaring `text/event-stream`; any operation that declares it is excluded too.
 */
val excludedOperations = setOf("replyInConversation", "resumeConversation", "streamCrewLive")

/** Operations whose read timeout is 300 s (docs/design.md 7.3). */
val longOperations =
    setOf(
        "createPlan",
        "parseWorkoutImport",
        "parseMealImport",
        "generateMealPlan",
        "estimateProgressCheck",
        "compareProgressChecks",
        "decideToolCalls",
        "uploadChatAttachment",
    )

/** Counts at the pinned contract. A change means the spec moved: check the new operations, then update these. */
val expectedRemoved = 12
val expectedOperations = 378
val expectedInterfaces = 68

val normalizeSpec by tasks.registering {
    val input = contractDir.file("openapi.yaml")
    val spec = normalizedSpec
    val operations = operationsList
    val excluded = excludedOperations
    val long = longOperations
    val expected = Triple(expectedRemoved, expectedOperations, expectedInterfaces)
    inputs.file(input)
    outputs.files(spec, operations)
    doLast {
        val raw = input.asFile.readText()
        val yaml11Key = Regex("""(?m)^\s+(off|yes|no|y|n|On|Off|Yes|No|Y|N|ON|OFF|YES|NO):""")
        yaml11Key.find(raw)?.let {
            throw GradleException(
                "normalizeSpec: YAML 1.1 reads key '${it.groupValues[1]}' as a boolean; quote it here"
            )
        }
        val text =
            raw.replace(Regex("""(?m)^(\s+)on:""")) { "${it.groupValues[1]}'on':" }
                .replace(Regex("""(?m)^(\s+required: \[[^\]]*)\bon\b""")) { "${it.groupValues[1]}'on'" }
        val mapper = YAMLMapper()
        val root = mapper.readTree(text) as ObjectNode
        val paths = root.get("paths") as ObjectNode
        val methods = listOf("get", "put", "post", "patch", "delete", "head", "options")
        val index = StringBuilder()
        val removed = mutableListOf<String>()
        val tags = mutableSetOf<String>()
        var kept = 0
        paths.fieldNames().asSequence().toList().forEach { path ->
            val item = paths.get(path) as ObjectNode
            methods.forEach { m ->
                val op = item.get(m) as? ObjectNode ?: return@forEach
                val id = op.path("operationId").asText()
                val security = op.get("security")
                val isPublic = security is ArrayNode && security.isEmpty && id != "logOut"
                index.append("${m.uppercase()}\t$path\t$id\t$isPublic\t${id in long}\n")
                val multipart = op.path("requestBody").path("content").has("multipart/form-data")
                val stream = op.path("responses").properties().any { it.value.path("content").has("text/event-stream") }
                if (id in excluded || multipart || stream) {
                    item.remove(m)
                    removed += "$id${if (multipart) " (multipart)" else if (stream) " (stream)" else ""}"
                } else {
                    val tag = path.trim('/').substringBefore('/').ifEmpty { "root" }
                    op.putArray("tags").add(tag)
                    tags += tag
                    kept++
                }
            }
            if (methods.none { item.has(it) }) paths.remove(path)
        }
        var nullable = 0
        var freeForm = 0
        fun walk(node: JsonNode) {
            if (node is ObjectNode) {
                if (
                    node.path("type").asText() == "object" &&
                        node.path("additionalProperties").asBoolean(false) &&
                        !node.has("properties")
                ) {
                    node.remove("additionalProperties")
                    freeForm++
                }
                if (node.path("nullable").asBoolean(false)) {
                    node.remove("nullable")
                    val allOf = node.get("allOf") as? ArrayNode
                    if (allOf != null && allOf.size() == 1) {
                        node.remove("allOf")
                        node.putArray("anyOf").apply {
                            add(allOf.get(0))
                            addObject().put("type", "null")
                        }
                    } else if (node.get("type")?.isTextual == true) {
                        val type = node.get("type").asText()
                        node.putArray("type").add(type).add("null")
                    } else {
                        throw GradleException("normalizeSpec: unhandled nullable shape: $node")
                    }
                    nullable++
                }
                node.properties().forEach { walk(it.value) }
            } else if (node is ArrayNode) {
                node.forEach { walk(it) }
            }
        }
        walk(root)
        val counts = Triple(removed.size, kept, tags.size)
        if (counts != expected) {
            throw GradleException(
                "normalizeSpec: removed/kept/interfaces = $counts, expected $expected. The spec changed: review it, then " +
                    "update the expected counts in core/api/build.gradle.kts. Removed: $removed"
            )
        }
        spec.get().asFile.apply { parentFile.mkdirs() }.writeText(mapper.writeValueAsString(root))
        operations.get().asFile.writeText(index.toString())
        logger.lifecycle(
            "normalizeSpec: removed $removed; $kept operations in ${tags.size} interfaces; " +
                "$nullable nullable sites; $freeForm free-form objects"
        )
    }
}

val generateOperationIndex by tasks.registering {
    val operations = operationsList
    val out = operationIndexDir
    dependsOn(normalizeSpec)
    inputs.file(operations)
    outputs.dir(out)
    doLast {
        val rows =
            operations
                .get()
                .asFile
                .readLines()
                .filter { it.isNotBlank() }
                .joinToString(",\n") { line ->
                    val (method, path, id, isPublic, isLong) = line.split("\t")
                    "    OperationEntry(\"$method\", \"$path\", \"$id\", $isPublic, $isLong)"
                }
        val file = out.get().file("com/fernandocorreia/khepri/core/api/GeneratedOperations.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            "// Generated from the contract by :core:api:generateOperationIndex. Do not edit.\n" +
                "package com.fernandocorreia.khepri.core.api\n\n" +
                "internal val generatedOperations: List<OperationEntry> = listOf(\n$rows,\n)\n"
        )
    }
}

val generateClient by
    tasks.registering(GenerateTask::class) {
        dependsOn(normalizeSpec)
        generatorName.set("kotlin")
        library.set("jvm-retrofit2")
        inputSpec.set(normalizedSpec)
        outputDir.set(generatedDir)
        packageName.set("com.fernandocorreia.khepri.core.api")
        apiPackage.set("com.fernandocorreia.khepri.core.api.endpoint")
        modelPackage.set("com.fernandocorreia.khepri.core.api.model")
        modelNameSuffix.set("Dto")
        generateApiTests.set(false)
        generateModelTests.set(false)
        generateApiDocumentation.set(false)
        generateModelDocumentation.set(false)
        ignoreFileOverride.set(layout.projectDirectory.file("openapi-generator-ignore").asFile.absolutePath)
        enumNameMappings.set(mapOf("" to "EMPTY", "unknown_default_open_api" to "UNKNOWN"))
        typeMappings.set(
            mapOf(
                "DateTime" to "java.time.Instant",
                "UUID" to "kotlin.String",
                "AnyType" to "JsonElement",
                "object" to "JsonObject",
                "number" to "kotlin.Double",
                "URI" to "kotlin.String",
            )
        )
        importMappings.set(
            mapOf(
                "JsonElement" to "kotlinx.serialization.json.JsonElement",
                "JsonObject" to "kotlinx.serialization.json.JsonObject",
            )
        )
        configOptions.set(
            mapOf(
                "serializationLibrary" to "kotlinx_serialization",
                "useCoroutines" to "true",
                "enumUnknownDefaultCase" to "true",
                "enumPropertyNaming" to "UPPERCASE",
                "dateLibrary" to "java8",
                "sourceFolder" to "src/main/kotlin",
                "omitGradleWrapper" to "true",
                "omitGradlePluginVersions" to "true",
            )
        )
    }

sourceSets.main {
    kotlin.srcDir(files(generatedDir.map { it.dir("src/main/kotlin") }).builtBy(generateClient))
    kotlin.srcDir(files(operationIndexDir).builtBy(generateOperationIndex))
}

tasks.named("compileKotlin") { dependsOn(generateClient, generateOperationIndex) }

tasks.withType<Detekt>().configureEach { exclude { it.file.absolutePath.contains("/build/") } }

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.okhttp)

    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.json.schema.validator)
    testImplementation(libs.jackson.yaml)
    testImplementation(kotlin("reflect"))
}

tasks.test {
    systemProperty("contract.dir", contractDir.asFile.absolutePath)
    systemProperty("normalized.spec", normalizedSpec.get().asFile.absolutePath)
    inputs.dir(contractDir)
    inputs.file(normalizedSpec)
    systemProperty(
        "generated.endpoints",
        generatedDir.get().dir("src/main/kotlin/com/fernandocorreia/khepri/core/api/endpoint").asFile.absolutePath,
    )
    systemProperty(
        "generated.models",
        generatedDir.get().dir("src/main/kotlin/com/fernandocorreia/khepri/core/api/model").asFile.absolutePath,
    )
}
