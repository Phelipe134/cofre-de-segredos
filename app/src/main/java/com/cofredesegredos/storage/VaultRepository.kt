package com.cofredesegredos.storage

import com.cofredesegredos.models.VaultData

interface VaultRepository {
    fun isInitialized(): Boolean
    fun createStore(password: String): VaultData
    fun unlock(password: String): VaultData
    fun save(data: VaultData)
    fun exportBackup(): String
    fun importBackup(rawBackup: String)
    fun clearSession()
}
