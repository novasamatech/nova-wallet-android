package io.novafoundation.nova.feature_wallet_api.data.repository

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceLock
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow

interface BalanceLocksRepository {

    suspend fun observeBalanceLocks(metaId: Long, chain: Chain, chainAsset: Chain.Asset): Flow<List<BalanceLock>>

    suspend fun getBiggestLock(chain: Chain, chainAsset: Chain.Asset): BalanceLock?

    suspend fun observeBalanceLock(chainAsset: Chain.Asset, lockId: String): Flow<BalanceLock?>

    fun observeLocksForMetaAccount(metaAccount: MetaAccount): Flow<List<BalanceLock>>
}
