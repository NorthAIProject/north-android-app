package com.fernandocorreia.khepri.core.api

/** Golden file name to schema name, from `golden-map.txt`. */
object TestMapping {
    /** Goldens that are not API responses: the MCP server's tool lists (`internal/mcpserver`). */
    val notApiResponses = setOf("tools", "tools.readonly")

    val entries: Map<String, String> by lazy {
        requireNotNull(javaClass.getResource("/golden-map.txt"))
            .readText()
            .lines()
            .filter { it.isNotBlank() }
            .associate {
                val (golden, schema) = it.trim().split(" ")
                golden to schema
            }
    }

    fun typeOf(golden: String): String = requireNotNull(entries[golden]) { "no mapping for $golden" }
}
