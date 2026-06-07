package com.cofredesegredos.models

import org.json.JSONObject
import java.util.UUID

data class Secret(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val login: String,
    val password: String,
    val notes: String,
    val customLabel: String,
    val customValue: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("title", title)
        .put("login", login)
        .put("password", password)
        .put("notes", notes)
        .put("customLabel", customLabel)
        .put("customValue", customValue)
        .put("createdAt", createdAt)

    companion object {
        fun fromJson(json: JSONObject): Secret = Secret(
            id = json.optString("id"),
            title = json.optString("title"),
            login = json.optString("login"),
            password = json.optString("password"),
            notes = json.optString("notes"),
            customLabel = json.optString("customLabel"),
            customValue = json.optString("customValue"),
            createdAt = json.optLong("createdAt", System.currentTimeMillis())
        )
    }
}
