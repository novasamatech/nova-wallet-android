package io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.store

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.selectionStore.ComputationalCacheSelectionStoreProvider
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.RecommendableMultiStakingSelection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

fun StartMultiStakingSelectionStoreProvider.currentSelectionFlow(scope: CoroutineScope): Flow<RecommendableMultiStakingSelection?> {
    return flowOfAll {
        getSelectionStore(scope).currentSelectionFlow
    }
}

class StartMultiStakingSelectionStoreProvider(
    computationalCache: ComputationalCache,
    key: String
) : ComputationalCacheSelectionStoreProvider<StartMultiStakingSelectionStore>(computationalCache, key) {

    protected override fun initSelectionStore(): StartMultiStakingSelectionStore {
        return StartMultiStakingSelectionStore()
    }
}
