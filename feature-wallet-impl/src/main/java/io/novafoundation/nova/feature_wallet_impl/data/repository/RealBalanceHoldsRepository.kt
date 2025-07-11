package io.novafoundation.nova.feature_wallet_impl.data.repository

import android.util.Log
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.balances
import io.novafoundation.nova.common.utils.getOrNull
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.core_db.dao.HoldsDao
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceHoldsRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceHold
import io.novafoundation.nova.feature_wallet_api.domain.model.mapBalanceHoldFromLocal
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chainsById
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.Struct
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.Vec
import io.novasama.substrate_sdk_android.runtime.metadata.storageOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RealBalanceHoldsRepository(
    private val chainRegistry: ChainRegistry,
    private val holdsDao: HoldsDao,
) : BalanceHoldsRepository {

    override suspend fun chainHasHoldId(chainId: ChainId, holdId: BalanceHold.HoldId): Boolean {
        return runCatching {
            val holdReasonType = getHoldReasonType(chainId) ?: return false
            holdReasonType.hasHoldId(holdId).also {
                Log.d(LOG_TAG, "chainHasHoldId for $chainId: $it")
            }
        }
            .onFailure {  Log.w(LOG_TAG, "Failed to get hold reason type", it) }
            .getOrDefault(false)
    }

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

    private fun DictEnum.hasHoldId(holdId: BalanceHold.HoldId): Boolean {
        val moduleReasons = getOrNull(holdId.module) as? DictEnum ?: return false
        return moduleReasons[holdId.reason] != null
    }

    private suspend fun getHoldReasonType(chainId: ChainId): DictEnum? {
        val runtime = chainRegistry.getRuntime(chainId)

        val storage = runtime.metadata.balances().storageOrNull("Holds") ?: return null
        val storageReturnType = storage.type.value as Vec

        return storageReturnType
            .innerType<Struct>()!!
            .get<DictEnum>("id")
    }
}
