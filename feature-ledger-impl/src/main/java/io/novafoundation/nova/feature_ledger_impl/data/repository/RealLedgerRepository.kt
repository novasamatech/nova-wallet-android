package io.novafoundation.nova.feature_ledger_impl.data.repository

import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.feature_ledger_api.data.repository.LedgerDerivationPath
import io.novafoundation.nova.feature_ledger_api.data.repository.LedgerRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class RealLedgerRepository(
    private val secretStoreV2: SecretStoreV2,
) : LedgerRepository {

    override suspend fun getChainAccountDerivationPath(metaId: Long, chainId: ChainId): String {
        val key = LedgerDerivationPath.derivationPathSecretKey(chainId)

        return secretStoreV2.getAdditionalMetaAccountSecret(metaId, key)
            ?: throw IllegalStateException("Cannot find Ledger derivation path for chain $chainId in meta account $metaId")
    }
}
