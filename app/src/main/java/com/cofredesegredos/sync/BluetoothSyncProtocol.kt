package com.cofredesegredos.sync

import java.util.UUID

object BluetoothSyncProtocol {
    val SERVICE_UUID: UUID = UUID.fromString("9d8f2b20-8a9a-4f31-a832-79b79d53a901")
    val CONTROL_UUID: UUID = UUID.fromString("9d8f2b21-8a9a-4f31-a832-79b79d53a901")
    val DATA_UUID: UUID = UUID.fromString("9d8f2b22-8a9a-4f31-a832-79b79d53a901")
    val CCC_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    const val CHUNK_SIZE = 16
}
