package io.novafoundation.nova.feature_wallet_api.data.repository

import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceHold
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow

interface BalanceHoldsRepository {

    suspend fun observeBalanceHolds(metaInt: Long, chainAsset: Chain.Asset): Flow<List<BalanceHold>>

    fun observeHoldsForMetaAccount(metaInt: Long): Flow<List<BalanceHold>>
}
