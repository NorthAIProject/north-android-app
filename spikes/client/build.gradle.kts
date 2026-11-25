import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.openapi.generator)
}

kotlin { jvmToolchain(21) }

val contractDir = rootProject.layout.projectDirectory.dir("contract")
val normalizedSpec = layout.buildDirectory.file("normalized/openapi.yaml")
val generatedDir = layout.buildDirectory.dir("generated/openapi")

/** Operations the generator must not see: event streams are hand-written, multipart is hand-written. */
val excludedOperations = setOf("replyInConversation", "resumeConversation", "streamCrewLive")

val normalizeSpec by tasks.registering {
    val input = contractDir.file("openapi.yaml")
    inputs.file(input)
    outputs.file(normalizedSpec)
    doLast {
        val mapper = YAMLMapper()
        // YAML 1.1 parsers read a bare `on` as boolean true; the spec uses `on` as a property name.
        val text =
            input.asFile
                .readText()
                .replace(Regex("""(?m)^(\s+)on:""")) { "${it.groupValues[1]}'on':" }
                .replace(Regex("""(?m)^(\s+required: \[[^\]]*)\bon\b""")) { "${it.groupValues[1]}'on'" }
        val root = mapper.readTree(text) as ObjectNode
        val removed = mutableListOf<String>()
        val paths = root.get("paths") as ObjectNode
        val methods = listOf("get", "put", "post", "patch", "delete", "head", "options")
        paths.fieldNames().asSequence().toList().forEach { path ->
            val item = paths.get(path) as ObjectNode
            methods.forEach { m ->
                val op = item.get(m) as? ObjectNode ?: return@forEach
                val id = op.path("operationId").asText()
                val multipart = op.path("requestBody").path("content").has("multipart/form-data")
                if (id in excludedOperations || multipart) {
                    item.remove(m)
                    removed += "$id${if (multipart) " (multipart)" else ""}"
                }
            }
            if (methods.none { item.has(it) }) paths.remove(path)
        }
        // OpenAPI 3.1 has no `nullable`; rewrite `nullable: true` + allOf[$ref] into anyOf[$ref, null].
        var rewritten = 0
        var freeForm = 0
        fun walk(node: com.fasterxml.jackson.databind.JsonNode) {
            if (node is ObjectNode) {
                // A free-form object (no properties, any keys) is generated as JsonObject, not Map<String, Any>.
                if (node.path("type").asText() == "object" &&
                    node.path("additionalProperties").isBoolean && !node.has("properties")
                ) {
                    node.remove("additionalProperties")
                    freeForm++
                }
                if (node.path("nullable").asBoolean(false)) {
                    node.remove("nullable")
                    val allOf = node.get("allOf") as? ArrayNode
                    if (allOf != null && allOf.size() == 1) {
                        node.remove("allOf")
                        val anyOf = node.putArray("anyOf")
                        anyOf.add(allOf.get(0))
                        anyOf.addObject().put("type", "null")
                    } else if (node.get("type")?.isTextual == true) {
                        val t = node.get("type").asText()
                        node.putArray("type").add(t).add("null")
                    }
                    rewritten++
                }
                node.fields().forEach { walk(it.value) }
            } else if (node is ArrayNode) node.forEach { walk(it) }
        }
        walk(root)
        normalizedSpec.get().asFile.apply { parentFile.mkdirs() }.writeText(mapper.writeValueAsString(root))
        logger.lifecycle("normalizeSpec: removed ${removed.size} operations: $removed; rewrote $rewritten nullable sites; $freeForm free-form objects")
    }
}

val generateClient by tasks.registering(GenerateTask::class) {
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

sourceSets.main { kotlin.srcDir(generatedDir.map { it.dir("src/main/kotlin") }) }

tasks.named("compileKotlin") { dependsOn(generateClient) }

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.json.schema.validator)
    testImplementation(libs.jackson.yaml)
    testImplementation(kotlin("reflect"))
}

tasks.test {
    useJUnitPlatform()
    systemProperty("contract.dir", contractDir.asFile.absolutePath)
    systemProperty("normalized.spec", normalizedSpec.get().asFile.absolutePath)
    testLogging { events("failed"); exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
}
