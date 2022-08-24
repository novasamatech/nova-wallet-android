package io.novafoundation.nova.feature_ledger_api.sdk.application.substrate

import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface SubstrateLedgerApplication {

    suspend fun getAccount(
        device: LedgerDevice,
        chainId: ChainId,
        accountIndex: Int,
        confirmAddress: Boolean
    ): LedgerSubstrateAccount

    suspend fun getSignature(
        device: LedgerDevice,
        chainId: ChainId,
        accountIndex: Int,
        payload: ByteArray,
    ): ByteArray
}
