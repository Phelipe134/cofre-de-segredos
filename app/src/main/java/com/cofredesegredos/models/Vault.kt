package com.cofredesegredos.models

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Vault(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val iconKey: String = "keys",
    val colorKey: String = "blue",
    val secrets: List<Secret> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("description", description)
        .put("iconKey", iconKey)
        .put("colorKey", colorKey)
        .put("createdAt", createdAt)
        .put("secrets", JSONArray().also { array ->
            secrets.forEach { array.put(it.toJson()) }
        })

    companion object {
        fun fromJson(json: JSONObject): Vault {
            val secretsJson = json.optJSONArray("secrets") ?: JSONArray()
            val secrets = buildList {
                for (index in 0 until secretsJson.length()) {
                    add(Secret.fromJson(secretsJson.getJSONObject(index)))
                }
            }
            return Vault(
                id = json.optString("id"),
                name = json.optString("name"),
                description = json.optString("description"),
                iconKey = json.optString("iconKey", "keys"),
                colorKey = json.optString("colorKey", "blue"),
                secrets = secrets,
                createdAt = json.optLong("createdAt", System.currentTimeMillis())
            )
        }
    }
}
