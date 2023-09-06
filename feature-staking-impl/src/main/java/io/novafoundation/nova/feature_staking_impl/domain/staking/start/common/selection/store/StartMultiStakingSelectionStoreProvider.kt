package io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.store

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.RecommendableMultiStakingSelection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface StartMultiStakingSelectionStoreProvider {

    suspend fun getSelectionStore(scope: CoroutineScope): StartMultiStakingSelectionStore
}

fun StartMultiStakingSelectionStoreProvider.currentSelectionFlow(scope: CoroutineScope): Flow<RecommendableMultiStakingSelection?> {
    return flowOfAll {
        getSelectionStore(scope).currentSelectionFlow
    }
}

class RealStartMultiStakingSelectionStoreProvider(
    private val computationalCache: ComputationalCache,
    private val key: String
) : StartMultiStakingSelectionStoreProvider {

    override suspend fun getSelectionStore(scope: CoroutineScope): StartMultiStakingSelectionStore {
        return computationalCache.useCache(key, scope) {
            RealStartMultiStakingSelectionStore()
        }
    }
}
