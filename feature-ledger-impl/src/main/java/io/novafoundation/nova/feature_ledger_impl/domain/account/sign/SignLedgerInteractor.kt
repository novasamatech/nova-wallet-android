package io.novafoundation.nova.feature_ledger_impl.domain.account.sign

import io.novafoundation.nova.common.utils.chainId
import io.novafoundation.nova.feature_account_api.data.signer.SeparateFlowSignerState
import io.novafoundation.nova.feature_account_api.data.signer.chainId
import io.novafoundation.nova.feature_account_api.data.signer.signaturePayload
import io.novafoundation.nova.feature_account_api.domain.model.LedgerVariant
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_impl.domain.migration.LedgerMigrationUseCase
import io.novafoundation.nova.runtime.ext.verifyMultiChain
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novasama.substrate_sdk_android.encrypt.SignatureVerifier
import io.novasama.substrate_sdk_android.encrypt.SignatureWrapper
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.InheritedImplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface SignLedgerInteractor {

    suspend fun getSignature(
        device: LedgerDevice,
        metaId: Long,
        payload: InheritedImplication,
    ): SignatureWrapper

    suspend fun verifySignature(
        payload: SeparateFlowSignerState,
        signature: SignatureWrapper
    ): Boolean
}

class RealSignLedgerInteractor(
    private val chainRegistry: ChainRegistry,
    private val usedVariant: LedgerVariant,
    private val migrationUseCase: LedgerMigrationUseCase
) : SignLedgerInteractor {

    override suspend fun getSignature(
        device: LedgerDevice,
        metaId: Long,
        payload: InheritedImplication
    ): SignatureWrapper = withContext(Dispatchers.Default) {
        val chainId = payload.chainId
        val app = migrationUseCase.determineLedgerApp(chainId, usedVariant)

        app.getSignature(device, metaId, chainId, payload)
    }

    override suspend fun verifySignature(
        payload: SeparateFlowSignerState,
        signature: SignatureWrapper
    ): Boolean = runCatching {
        val payloadBytes = payload.payload.signaturePayload()
        val chainId = payload.payload.chainId()
        val chain = chainRegistry.getChain(chainId)

        val publicKey = payload.metaAccount.publicKeyIn(chain) ?: throw IllegalStateException("No public key for chain $chainId")

        SignatureVerifier.verifyMultiChain(chain, signature, payloadBytes, publicKey)
    }.getOrDefault(false)
}
