package com.cofredesegredos.storage

import android.content.Context

class CredentialActivityStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun recentIds(): List<String> =
        preferences.getString(KEY_RECENTS, "").orEmpty()
            .split(SEPARATOR)
            .filter { it.isNotBlank() }

    fun recordRecent(vaultId: String, secretId: String) {
        val id = compoundId(vaultId, secretId)
        val updated = listOf(id) + recentIds().filterNot { it == id }
        preferences.edit()
            .putString(KEY_RECENTS, updated.take(MAX_RECENTS).joinToString(SEPARATOR))
            .apply()
    }

    fun favoriteIds(): Set<String> =
        preferences.getStringSet(KEY_FAVORITES, emptySet()).orEmpty()

    fun isFavorite(vaultId: String, secretId: String): Boolean =
        compoundId(vaultId, secretId) in favoriteIds()

    fun toggleFavorite(vaultId: String, secretId: String): Boolean {
        val id = compoundId(vaultId, secretId)
        val updated = favoriteIds().toMutableSet()
        val favorite = if (id in updated) {
            updated.remove(id)
            false
        } else {
            updated.add(id)
            true
        }
        preferences.edit().putStringSet(KEY_FAVORITES, updated).apply()
        return favorite
    }

    fun compoundId(vaultId: String, secretId: String): String = "$vaultId$ID_SEPARATOR$secretId"

    companion object {
        private const val PREFERENCES_NAME = "cofre_credential_activity"
        private const val KEY_RECENTS = "recent_ids"
        private const val KEY_FAVORITES = "favorite_ids"
        private const val SEPARATOR = ","
        private const val ID_SEPARATOR = "|"
        private const val MAX_RECENTS = 12
    }
}
