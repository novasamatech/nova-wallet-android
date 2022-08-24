package io.novafoundation.nova.feature_ledger_api.sdk.device

import io.novafoundation.nova.feature_ledger_api.sdk.connection.LedgerConnection
import kotlinx.coroutines.flow.first

class LedgerDevice(
    val id: String,
    val name: String,
    val connection: LedgerConnection,
)

suspend fun LedgerDevice.awaitConnected() = connection.isActive.first { connected -> connected }
