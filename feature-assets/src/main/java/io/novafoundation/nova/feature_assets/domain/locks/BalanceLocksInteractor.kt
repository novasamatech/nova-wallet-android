package io.novafoundation.nova.feature_assets.domain.locks

import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceHold
import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceLock
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.flow.Flow

interface BalanceLocksInteractor {

    fun balanceLocksFlow(chainId: ChainId, chainAssetId: Int): Flow<List<BalanceLock>>

    fun balanceHoldsFlow(chainId: ChainId, chainAssetId: Int): Flow<List<BalanceHold>>
}
