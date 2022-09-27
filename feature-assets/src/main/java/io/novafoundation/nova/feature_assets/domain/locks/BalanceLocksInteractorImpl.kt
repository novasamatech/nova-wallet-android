package io.novafoundation.nova.feature_assets.domain.locks

import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceLock
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chainWithAsset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

class BalanceLocksInteractorImpl(
    private val chainRegistry: ChainRegistry,
    private val balanceLocksRepository: BalanceLocksRepository,
) : BalanceLocksInteractor {

    override fun balanceLocksFlow(chainId: ChainId, chainAssetId: Int): Flow<List<BalanceLock>> {
        return flow {
            val (chain, chainAsset) = chainRegistry.chainWithAsset(chainId, chainAssetId)
            emitAll(balanceLocksRepository.observeBalanceLocks(chain, chainAsset))
        }
    }
}
