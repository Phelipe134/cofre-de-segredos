package com.cofredesegredos.sync

data class BluetoothSyncDiagnostics(
    val bluetoothEnabled: Boolean = false,
    val permissionsGranted: Boolean = false,
    val advertisingSupported: Boolean = false,
    val gattServerStarted: Boolean = false,
    val advertisingStarted: Boolean = false,
    val lastError: String = ""
)
