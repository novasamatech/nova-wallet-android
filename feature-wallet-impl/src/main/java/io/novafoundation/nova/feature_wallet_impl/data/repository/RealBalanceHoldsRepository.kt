package io.novafoundation.nova.feature_wallet_impl.data.repository

import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.core_db.dao.HoldsDao
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceHoldsRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceHold
import io.novafoundation.nova.feature_wallet_api.domain.model.mapBalanceHoldFromLocal
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chainsById
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RealBalanceHoldsRepository(
    private val chainRegistry: ChainRegistry,
    private val holdsDao: HoldsDao,
) : BalanceHoldsRepository {

    override suspend fun observeBalanceHolds(metaInt: Long, chainAsset: Chain.Asset): Flow<List<BalanceHold>> {
        return holdsDao.observeBalanceHolds(metaInt, chainAsset.chainId, chainAsset.id).mapList { hold ->
            mapBalanceHoldFromLocal(chainAsset, hold)
        }
    }

    override fun observeHoldsForMetaAccount(metaInt: Long): Flow<List<BalanceHold>> {
        return holdsDao.observeHoldsForMetaAccount(metaInt).map { holds ->
            val chainsById = chainRegistry.chainsById()
            holds.mapNotNull { holdLocal ->
                val asset = chainsById[holdLocal.chainId]?.assetsById?.get(holdLocal.assetId) ?: return@mapNotNull null

                mapBalanceHoldFromLocal(asset, holdLocal)
            }
        }
    }
}
