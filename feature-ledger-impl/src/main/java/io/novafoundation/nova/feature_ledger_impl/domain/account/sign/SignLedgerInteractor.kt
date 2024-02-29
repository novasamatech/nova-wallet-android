package io.novafoundation.nova.feature_ledger_impl.domain.account.sign

import io.novafoundation.nova.common.utils.chainId
import io.novafoundation.nova.feature_account_api.data.signer.SeparateFlowSignerState
import io.novafoundation.nova.feature_account_api.domain.model.publicKeyIn
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novasama.substrate_sdk_android.encrypt.SignatureVerifier
import io.novasama.substrate_sdk_android.encrypt.SignatureWrapper
import io.novasama.substrate_sdk_android.encrypt.Signer.MessageHashing
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.encodedSignaturePayload

interface SignLedgerInteractor {

    suspend fun verifySignature(
        payload: SeparateFlowSignerState,
        signature: SignatureWrapper
    ): Boolean
}

class RealSignLedgerInteractor(
    private val chainRegistry: ChainRegistry,
) : SignLedgerInteractor {

    override suspend fun verifySignature(
        payload: SeparateFlowSignerState,
        signature: SignatureWrapper
    ): Boolean = runCatching {
        val extrinsic = payload.extrinsic
        val payloadBytes = extrinsic.encodedSignaturePayload(hashBigPayloads = true)
        val chainId = extrinsic.chainId
        val chain = chainRegistry.getChain(chainId)

        val publicKey = payload.metaAccount.publicKeyIn(chain) ?: throw IllegalStateException("No public key for chain $chainId")

        SignatureVerifier.verify(signature, MessageHashing.SUBSTRATE, payloadBytes, publicKey)
    }.getOrDefault(false)
}
