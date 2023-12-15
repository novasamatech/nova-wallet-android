package io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import kotlinx.coroutines.CoroutineScope

private const val SETTINGS_PROVIDER_KEY = "SETTINGS_PROVIDER_KEY"

class RecommendationSettingsProviderFactory(
    private val computationalCache: ComputationalCache,
    private val chainRegistry: ChainRegistry,
    private val sharedState: StakingSharedState,
) {

    suspend fun create(scope: CoroutineScope): RecommendationSettingsProvider {
        return computationalCache.useCache(SETTINGS_PROVIDER_KEY, scope) {
            val chainId = sharedState.chainId()

            RecommendationSettingsProvider(chainRegistry.getRuntime(chainId))
        }
    }
}
