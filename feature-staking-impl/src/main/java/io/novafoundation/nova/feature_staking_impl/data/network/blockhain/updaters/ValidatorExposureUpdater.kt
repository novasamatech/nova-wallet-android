package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters

import io.novafoundation.nova.common.data.network.rpc.BulkRetriever
import io.novafoundation.nova.common.utils.hasStorage
import io.novafoundation.nova.common.utils.staking
import io.novafoundation.nova.core.model.StorageEntry
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_staking_api.domain.model.EraIndex
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.ValidatorExposureUpdater.Companion.STORAGE_KEY_PAGED_EXPOSURES
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.base.StakingUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.scope.ActiveEraScope
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey
import jp.co.soramitsu.fearless_utils.wsrpc.SocketService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import java.math.BigInteger

/**
 * Manages sync for validators exposures
 * Depending on the version of the staking pallet, exposures might be stored differently:
 *
 * 1. Legacy version - exposures are stored in `EraStakers` storage
 * 2. Current version - exposures are paged and stored in two storages: `EraStakersPaged` and `EraStakersOverview`
 *
 * Note that during the transition from Legacy to Current version during next [Staking.historyDepth]
 * eras older storage will still be present and filled with previous era information. Old storage will be cleared on era-by-era basis once new era happens.
 * Also, right after chain has just upgraded, `EraStakersPaged` will be empty untill next era happens.
 *
 * The updater takes care of that all also storing special [STORAGE_KEY_PAGED_EXPOSURES] indicating whether
 * paged exposures are actually present for the latest era or not
 */
class ValidatorExposureUpdater(
    private val bulkRetriever: BulkRetriever,
    private val stakingSharedState: StakingSharedState,
    private val chainRegistry: ChainRegistry,
    private val storageCache: StorageCache,
    override val scope: ActiveEraScope,
) : StakingUpdater<EraIndex> {

    companion object {

        const val STORAGE_KEY_PAGED_EXPOSURES = "NovaWallet.Staking.PagedExposuresUsed"

        fun isPagedExposuresValue(enabled: Boolean) = enabled.toString()

        fun decodeIsPagedExposuresValue(value: String?) = value.toBoolean()
    }

    override suspend fun listenForUpdates(
        storageSubscriptionBuilder: SharedRequestsBuilder,
        scopeValue: EraIndex,
    ): Flow<Updater.SideEffect> {
        @Suppress("UnnecessaryVariable")
        val activeEra = scopeValue
        val socketService = storageSubscriptionBuilder.socketService ?: return emptyFlow()

        return flow<Updater.SideEffect> {
            val chainId = stakingSharedState.chainId()
            val runtime = chainRegistry.getRuntime(chainId)

            if (checkValuesInCache(activeEra, chainId, runtime)) {
                return@flow
            }

            cleanupOutdatedEras(chainId, runtime)

            syncNewExposures(activeEra, runtime, socketService, chainId)
        }.noSideAffects()
    }

    private suspend fun checkValuesInCache(era: BigInteger, chainId: String, runtimeSnapshot: RuntimeSnapshot): Boolean {
        if (runtimeSnapshot.pagedExposuresEnabled()) {
            return isPagedExposuresInCache(era, chainId, runtimeSnapshot) || isLegacyExposuresInCache(era, chainId, runtimeSnapshot)
        }

        return isLegacyExposuresInCache(era, chainId, runtimeSnapshot)
    }

    private suspend fun isPagedExposuresInCache(era: BigInteger, chainId: String, runtimeSnapshot: RuntimeSnapshot): Boolean {
        val prefix = runtimeSnapshot.eraStakersOverviewPrefixFor(era)

        return storageCache.isPrefixInCache(prefix, chainId)
    }

    private suspend fun isLegacyExposuresInCache(era: BigInteger, chainId: String, runtimeSnapshot: RuntimeSnapshot): Boolean {
        val prefix = runtimeSnapshot.eraStakersPrefixFor(era)

        return storageCache.isPrefixInCache(prefix, chainId)
    }

    private suspend fun cleanupOutdatedEras(chainId: String, runtimeSnapshot: RuntimeSnapshot) {
        if (runtimeSnapshot.pagedExposuresEnabled()) {
            cleanupPagedExposures(runtimeSnapshot, chainId)
        }

        cleanupLegacyExposures(runtimeSnapshot, chainId)
    }

    private suspend fun cleanupLegacyExposures(runtimeSnapshot: RuntimeSnapshot, chainId: String) {
        storageCache.removeByPrefix(runtimeSnapshot.eraStakersPrefix(), chainId)
    }

    private suspend fun cleanupPagedExposures(runtimeSnapshot: RuntimeSnapshot, chainId: String) {
        storageCache.removeByPrefix(runtimeSnapshot.eraStakersPagedPrefix(), chainId)
        storageCache.removeByPrefix(runtimeSnapshot.eraStakersOverviewPrefix(), chainId)
    }

    private suspend fun syncNewExposures(era: BigInteger, runtimeSnapshot: RuntimeSnapshot, socketService: SocketService, chainId: String) {
        var pagedExposureUsed = false

        if (runtimeSnapshot.pagedExposuresEnabled()) {
            val pagedExposuresWerePresent = tryFetchingPagedExposures(era, runtimeSnapshot, socketService, chainId)

            if (!pagedExposuresWerePresent) {
                fetchLegacyExposures(era, runtimeSnapshot, socketService, chainId)
            } else {
                pagedExposureUsed = true
            }
        } else {
            fetchLegacyExposures(era, runtimeSnapshot, socketService, chainId)
        }

        saveIsExposuresUsedFlag(pagedExposureUsed, chainId)
    }

    private suspend fun tryFetchingPagedExposures(
        era: BigInteger,
        runtimeSnapshot: RuntimeSnapshot,
        socketService: SocketService,
        chainId: String
    ): Boolean = runCatching {
        val overviewPrefix = runtimeSnapshot.eraStakersOverviewPrefixFor(era)
        val numberOfKeysSynced = bulkRetriever.fetchPrefixValuesToCache(socketService, overviewPrefix, storageCache, chainId)

        val pagedExposuresPresent = numberOfKeysSynced > 0

        if (pagedExposuresPresent) {
            val pagedExposuresPrefix = runtimeSnapshot.eraStakersPagedPrefixFor(era)
            bulkRetriever.fetchPrefixValuesToCache(socketService, pagedExposuresPrefix, storageCache, chainId)
        }

        pagedExposuresPresent
    }.getOrDefault(false)

    private suspend fun fetchLegacyExposures(
        era: BigInteger,
        runtimeSnapshot: RuntimeSnapshot,
        socketService: SocketService,
        chainId: String
    ): Result<Unit> = runCatching {
        val prefix = runtimeSnapshot.eraStakersPrefixFor(era)
        bulkRetriever.fetchPrefixValuesToCache(socketService, prefix, storageCache, chainId)
    }

    private suspend fun saveIsExposuresUsedFlag(isUsed: Boolean, chainId: String) {
        val encodedValue = isPagedExposuresValue(isUsed)
        val entry = StorageEntry(STORAGE_KEY_PAGED_EXPOSURES, encodedValue)

        storageCache.insert(entry, chainId)
    }

    private fun RuntimeSnapshot.pagedExposuresEnabled(): Boolean {
        return metadata.staking().hasStorage("ErasStakersPaged")
    }


    private fun RuntimeSnapshot.eraStakersPrefixFor(era: BigInteger): String {
        return metadata.staking().storage("ErasStakers").storageKey(this, era)
    }

    private fun RuntimeSnapshot.eraStakersOverviewPrefixFor(era: BigInteger): String {
        return metadata.staking().storage("ErasStakersOverview").storageKey(this, era)
    }

    private fun RuntimeSnapshot.eraStakersOverviewPrefix(): String {
        return metadata.staking().storage("ErasStakersOverview").storageKey(this)
    }

    private fun RuntimeSnapshot.eraStakersPagedPrefixFor(era: BigInteger): String {
        return metadata.staking().storage("ErasStakersPaged").storageKey(this, era)
    }

    private fun RuntimeSnapshot.eraStakersPagedPrefix(): String {
        return metadata.staking().storage("ErasStakersPaged").storageKey(this)
    }

    private fun RuntimeSnapshot.eraStakersPrefix(): String {
        return metadata.staking().storage("ErasStakers").storageKey(this)
    }


}
