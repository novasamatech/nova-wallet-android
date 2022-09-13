package io.novafoundation.nova.feature_ledger_api.sdk.application.substrate

import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.encrypt.SignatureWrapper
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadExtrinsic

interface SubstrateLedgerApplication {

    suspend fun getAccount(
        device: LedgerDevice,
        chainId: ChainId,
        accountIndex: Int,
        confirmAddress: Boolean
    ): LedgerSubstrateAccount

    suspend fun getSignature(
        device: LedgerDevice,
        metaId: Long,
        chainId: ChainId,
        payload: SignerPayloadExtrinsic,
    ): SignatureWrapper
}
