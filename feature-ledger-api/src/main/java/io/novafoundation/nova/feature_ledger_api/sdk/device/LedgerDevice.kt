package io.novafoundation.nova.feature_ledger_api.sdk.device

import io.novafoundation.nova.feature_ledger_api.sdk.connection.LedgerConnection

class LedgerDevice(
    val id: String,
    val deviceType: LedgerDeviceType,
    val name: String?,
    val connection: LedgerConnection,
) {

    override fun toString(): String {
        return "${name ?: id} ($deviceType)"
    }
}
