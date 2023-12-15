package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.historical

import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_staking_api.domain.model.EraIndex
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.base.StakingUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.scope.ActiveEraScope
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import kotlinx.coroutines.flow.Flow

interface HistoricalUpdater {

    fun constructKeyPrefix(runtime: RuntimeSnapshot): String
}

class HistoricalUpdateMediator(
    override val scope: ActiveEraScope,
    private val historicalUpdaters: List<HistoricalUpdater>,
    private val stakingSharedState: StakingSharedState,
    private val storageCache: StorageCache,
    private val chainRegistry: ChainRegistry,
    private val preferences: Preferences,
) : Updater<EraIndex>, StakingUpdater<EraIndex> {

    override suspend fun listenForUpdates(storageSubscriptionBuilder: SharedRequestsBuilder, scopeValue: EraIndex): Flow<Updater.SideEffect> {
        val chainId = stakingSharedState.chainId()
        val runtime = chainRegistry.getRuntime(chainId)

        return flowOf {
            if (isHistoricalDataCleared(chainId)) return@flowOf null

            val prefixes = historicalUpdaters.map { it.constructKeyPrefix(runtime) }
            prefixes.onEach { storageCache.removeByPrefix(prefixKey = it, chainId) }

            setHistoricalDataCleared(chainId)
        }
            .noSideAffects()
    }

    private fun isHistoricalDataCleared(chainId: ChainId): Boolean {
        return preferences.contains(isHistoricalDataClearedKey(chainId))
    }

    private fun setHistoricalDataCleared(chainId: ChainId) {
        preferences.putBoolean(isHistoricalDataClearedKey(chainId), true)
    }

    private fun isHistoricalDataClearedKey(chainId: ChainId): String {
        return "HistoricalUpdateMediator.HistoricalDataCleared::$chainId"
    }
}
