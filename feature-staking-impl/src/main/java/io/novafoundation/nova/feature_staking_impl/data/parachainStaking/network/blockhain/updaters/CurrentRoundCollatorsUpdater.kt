package io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.blockhain.updaters

import io.novafoundation.nova.common.data.network.rpc.BulkRetriever
import io.novafoundation.nova.common.utils.parachainStaking
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core.updater.GlobalScopeUpdater
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.RoundIndex
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.fetchPrefixValuesToCache
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.CurrentRoundRepository
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey
import jp.co.soramitsu.fearless_utils.wsrpc.SocketService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class CurrentRoundCollatorsUpdater(
    private val bulkRetriever: BulkRetriever,
    private val stakingSharedState: StakingSharedState,
    private val chainRegistry: ChainRegistry,
    private val storageCache: StorageCache,
    private val currentRoundRepository: CurrentRoundRepository,
) : GlobalScopeUpdater, ParachainStakingUpdater<Unit> {

    override suspend fun listenForUpdates(
        storageSubscriptionBuilder: SharedRequestsBuilder,
        scopeValue: Unit,
    ): Flow<Updater.SideEffect> {
        val socketService = storageSubscriptionBuilder.socketService ?: return emptyFlow()

        val chainId = stakingSharedState.chainId()
        val runtime = chainRegistry.getRuntime(chainId)

        return currentRoundRepository.currentRoundInfoFlow(chainId)
            .map { runtime.collatorSnapshotPrefixFor(it.current) }
            .filterNot { storageCache.isPrefixInCache(it, chainId) }
            .onEach { cleanupPreviousRounds(runtime, chainId) }
            .onEach { updateCollatorsPerRound(it, socketService, chainId) }
            .noSideAffects()
    }

    private suspend fun cleanupPreviousRounds(runtimeSnapshot: RuntimeSnapshot, chainId: String) {
        val prefix = runtimeSnapshot.collatorSnapshotPrefix()

        storageCache.removeByPrefix(prefix, chainId)
    }

    private fun RuntimeSnapshot.collatorSnapshotPrefix(): String {
        return metadata.parachainStaking().storage("AtStake").storageKey(this)
    }

    private fun RuntimeSnapshot.collatorSnapshotPrefixFor(roundIndex: RoundIndex): String {
        return metadata.parachainStaking().storage("AtStake").storageKey(this, roundIndex)
    }

    private suspend fun updateCollatorsPerRound(
        prefix: String,
        socketService: SocketService,
        chainId: String
    ) = runCatching {
        bulkRetriever.fetchPrefixValuesToCache(socketService, prefix, storageCache, chainId)
    }
}
