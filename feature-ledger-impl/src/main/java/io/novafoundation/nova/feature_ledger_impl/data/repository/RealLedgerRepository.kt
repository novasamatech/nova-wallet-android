package io.novafoundation.nova.feature_ledger_impl.data.repository

import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.feature_ledger_api.data.repository.LedgerRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

private const val LEDGER_DERIVATION_PATH_KEY = "LedgerChainAccount.derivationPath"

class RealLedgerRepository(
    private val secretStoreV2: SecretStoreV2,
) : LedgerRepository {

    override suspend fun getChainAccountDerivationPath(metaId: Long, chainId: ChainId): String {
        val key = derivationPathSecretKey(chainId)

        return secretStoreV2.getAdditionalMetaAccountSecret(metaId, key)
            ?: throw IllegalStateException("Cannot find Ledger derivation path for chain $chainId in meta account $metaId")
    }

    override fun derivationPathSecretKey(chainId: ChainId): String {
        return "$LEDGER_DERIVATION_PATH_KEY.$chainId"
    }
}
