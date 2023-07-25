package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.historical

import io.novafoundation.nova.common.data.network.rpc.BulkRetriever
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core.updater.GlobalScopeUpdater
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_api.domain.api.historicalEras
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.fetchValuesToCache
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.observeActiveEraIndex
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.math.BigInteger

interface HistoricalUpdater {

    fun constructHistoricalKey(runtime: RuntimeSnapshot, era: BigInteger): String

    fun constructKeyPrefix(runtime: RuntimeSnapshot): String
}

class HistoricalUpdateMediator(
    private val historicalUpdaters: List<HistoricalUpdater>,
    private val stakingSharedState: StakingSharedState,
    private val bulkRetriever: BulkRetriever,
    private val stakingRepository: StakingRepository,
    private val storageCache: StorageCache,
    private val chainRegistry: ChainRegistry,
) : GlobalScopeUpdater {

    override val requiredModules: List<String> = listOf(Modules.STAKING)

    override suspend fun listenForUpdates(storageSubscriptionBuilder: SharedRequestsBuilder, scopeValue: Unit): Flow<Updater.SideEffect> {
        val chainId = stakingSharedState.chainId()
        val runtime = chainRegistry.getRuntime(chainId)

        val socketService = storageSubscriptionBuilder.socketService ?: return emptyFlow()

        return storageCache.observeActiveEraIndex(runtime, chainId)
            .map {
                val allKeysNeeded = constructHistoricalKeys(chainId, runtime)
                val keysInDataBase = storageCache.filterKeysInCache(allKeysNeeded, chainId).toSet()

                val missingKeys = allKeysNeeded.filter { it !in keysInDataBase }

                allKeysNeeded to missingKeys
            }
            .filter { (_, missing) -> missing.isNotEmpty() }
            .onEach { (allNeeded, missing) ->
                val prefixes = historicalUpdaters.map { it.constructKeyPrefix(runtime) }
                prefixes.onEach { storageCache.removeByPrefixExcept(prefixKey = it, fullKeyExceptions = allNeeded, chainId) }

                bulkRetriever.fetchValuesToCache(socketService, missing, storageCache, chainId)
            }
            .noSideAffects()
    }

    private suspend fun constructHistoricalKeys(chainId: ChainId, runtime: RuntimeSnapshot): List<String> {
        val historicalRange = stakingRepository.historicalEras(chainId)

        return historicalUpdaters.map { updater ->
            historicalRange.map { updater.constructHistoricalKey(runtime, it) }
        }.flatten()
    }
}
