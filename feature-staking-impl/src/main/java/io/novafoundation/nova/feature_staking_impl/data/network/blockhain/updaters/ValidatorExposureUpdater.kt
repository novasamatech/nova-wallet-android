package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters

import io.novafoundation.nova.common.data.network.rpc.BulkRetriever
import io.novafoundation.nova.common.utils.staking
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core.updater.GlobalScopeUpdater
import io.novafoundation.nova.core.updater.SubscriptionBuilder
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.base.StakingUpdater
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey
import jp.co.soramitsu.fearless_utils.wsrpc.SocketService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.math.BigInteger

class ValidatorExposureUpdater(
    private val bulkRetriever: BulkRetriever,
    private val stakingSharedState: StakingSharedState,
    private val chainRegistry: ChainRegistry,
    private val storageCache: StorageCache
) : GlobalScopeUpdater, StakingUpdater {

    override suspend fun listenForUpdates(storageSubscriptionBuilder: SubscriptionBuilder): Flow<Updater.SideEffect> {
        val chainId = stakingSharedState.chainId()
        val runtime = chainRegistry.getRuntime(chainId)

        return storageCache.observeActiveEraIndex(runtime, chainId)
            .map { activeEraIndex -> runtime.eraStakersPrefixFor(activeEraIndex) }
            .filterNot { storageCache.isPrefixInCache(it, chainId) }
            .onEach { cleanupPreviousEras(runtime, chainId) }
            .onEach { updateNominatorsForEra(it, storageSubscriptionBuilder.socketService, chainId) }
            .flowOn(Dispatchers.IO)
            .noSideAffects()
    }

    private suspend fun cleanupPreviousEras(runtimeSnapshot: RuntimeSnapshot, chainId: String) {
        val prefix = runtimeSnapshot.eraStakersPrefix()

        storageCache.removeByPrefix(prefix, chainId)
    }

    private fun RuntimeSnapshot.eraStakersPrefixFor(era: BigInteger): String {
        return metadata.staking().storage("ErasStakers").storageKey(this, era)
    }

    private fun RuntimeSnapshot.eraStakersPrefix(): String {
        return metadata.staking().storage("ErasStakers").storageKey(this)
    }

    private suspend fun updateNominatorsForEra(
        eraStakersPrefix: String,
        socketService: SocketService,
        chainId: String
    ) = runCatching {
        bulkRetriever.fetchPrefixValuesToCache(socketService, eraStakersPrefix, storageCache, chainId)
    }
}
