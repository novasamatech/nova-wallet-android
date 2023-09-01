package io.novafoundation.nova.feature_wallet_api.data.repository

import io.novafoundation.nova.feature_wallet_api.domain.model.ExternalBalance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.flow.Flow

interface ExternalBalanceRepository {

    fun observeAccountExternalBalances(metaId: Long): Flow<List<ExternalBalance>>

    fun observeAccountChainExternalBalances(metaId: Long, assetId: FullChainAssetId): Flow<List<ExternalBalance>>
}
