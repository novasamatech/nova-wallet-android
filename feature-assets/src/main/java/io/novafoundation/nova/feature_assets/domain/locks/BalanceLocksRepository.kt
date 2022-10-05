package io.novafoundation.nova.feature_assets.domain.locks

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceLock
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow

interface BalanceLocksRepository {

    suspend fun observeBalanceLocks(chain: Chain, chainAsset: Chain.Asset): Flow<List<BalanceLock>>

    fun observeLocksForMetaAccount(metaAccount: MetaAccount): Flow<List<BalanceLock>>
}
