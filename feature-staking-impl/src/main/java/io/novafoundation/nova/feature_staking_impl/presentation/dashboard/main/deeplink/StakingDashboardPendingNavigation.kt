package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.deeplink

import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Asset a staking deep link asked to open. The dashboard consumes it once its data has resolved
 * the asset, so a cold start still lands on it.
 */
class StakingDashboardPendingNavigation {

    private val _target = MutableStateFlow<FullChainAssetId?>(null)

    val target: Flow<FullChainAssetId?> = _target

    fun set(target: FullChainAssetId) {
        _target.value = target
    }

    fun consume(target: FullChainAssetId) {
        _target.compareAndSet(target, null)
    }
}
