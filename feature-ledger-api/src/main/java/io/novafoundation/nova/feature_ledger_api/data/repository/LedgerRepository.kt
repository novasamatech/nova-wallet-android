package io.novafoundation.nova.feature_ledger_api.data.repository

import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface LedgerRepository {

    suspend fun getChainAccountDerivationPath(
        metaId: Long,
        chainId: ChainId
    ): String

}
