package io.novafoundation.nova.feature_gift_api.domain

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface AvailableGiftAssetsUseCase {
    suspend fun isGiftsAvailable(chainAsset: Chain.Asset): Boolean

    fun getAvailableGiftAssets(coroutineScope: CoroutineScope): Flow<Set<FullChainAssetId>>
}
