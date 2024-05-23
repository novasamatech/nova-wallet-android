package io.novafoundation.nova.feature_ledger_impl.domain.account.sign

import io.novafoundation.nova.common.utils.chainId
import io.novafoundation.nova.feature_account_api.data.signer.SeparateFlowSignerState
import io.novafoundation.nova.feature_account_api.domain.model.LedgerVariant
import io.novafoundation.nova.feature_account_api.domain.model.publicKeyIn
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.SubstrateLedgerApplication
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_core.domain.LedgerMigrationTracker
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.legacyApp.LegacySubstrateLedgerApplication
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.newApp.GenericSubstrateLedgerApplication
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.newApp.MigrationSubstrateLedgerApplication
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.encrypt.SignatureVerifier
import io.novasama.substrate_sdk_android.encrypt.SignatureWrapper
import io.novasama.substrate_sdk_android.encrypt.Signer.MessageHashing
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignerPayloadExtrinsic
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.encodedSignaturePayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface SignLedgerInteractor {

    suspend fun getSignature(
        device: LedgerDevice,
        metaId: Long,
        payload: SignerPayloadExtrinsic,
    ): SignatureWrapper

    suspend fun verifySignature(
        payload: SeparateFlowSignerState,
        signature: SignatureWrapper
    ): Boolean
}

class RealSignLedgerInteractor(
    private val chainRegistry: ChainRegistry,
    private val usedVariant: LedgerVariant,
    private val migrationTracker: LedgerMigrationTracker,
    private val legacyApp: LegacySubstrateLedgerApplication,
    private val migrationApp: MigrationSubstrateLedgerApplication,
    private val genericApp: GenericSubstrateLedgerApplication,
) : SignLedgerInteractor {

    override suspend fun getSignature(device: LedgerDevice, metaId: Long, payload: SignerPayloadExtrinsic): SignatureWrapper = withContext(Dispatchers.Default) {
        val chainId = payload.chainId
        val app = determineLedgerApp(chainId)

        app.getSignature(device, metaId, chainId, payload)
    }

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

    private suspend fun determineLedgerApp(chainId: ChainId): SubstrateLedgerApplication {
        return when {
            usedVariant == LedgerVariant.GENERIC -> genericApp
            migrationTracker.shouldUseMigrationApp(chainId) -> migrationApp
            else -> legacyApp
        }
    }
}
