package io.novafoundation.nova.feature_ledger_api.sdk.application.substrate

import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.encrypt.SignatureWrapper
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.InheritedImplication

interface SubstrateLedgerApplication {

    suspend fun getSubstrateAccount(
        device: LedgerDevice,
        chainId: ChainId,
        accountIndex: Int,
        confirmAddress: Boolean
    ): LedgerSubstrateAccount

    suspend fun getEvmAccount(
        device: LedgerDevice,
        accountIndex: Int,
        confirmAddress: Boolean
    ): LedgerEvmAccount?

    suspend fun getSignature(
        device: LedgerDevice,
        metaId: Long,
        chainId: ChainId,
        payload: InheritedImplication,
    ): SignatureWrapper
}
