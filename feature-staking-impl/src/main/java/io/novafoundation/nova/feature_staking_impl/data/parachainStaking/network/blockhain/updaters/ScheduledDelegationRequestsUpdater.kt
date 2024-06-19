package io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.blockhain.updaters

import io.novafoundation.nova.common.utils.decodeValue
import io.novafoundation.nova.common.utils.parachainStaking
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core.storage.insert
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.bindDelegatorState
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.state.chainAndAsset
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.wrapSingleArgumentKeys
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import io.novasama.substrate_sdk_android.runtime.metadata.storageKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map

class ScheduledDelegationRequestsUpdater(
    override val scope: AccountUpdateScope,
    private val storageCache: StorageCache,
    private val stakingSharedState: StakingSharedState,
    private val remoteStorageDataSource: StorageDataSource,
    private val chainRegistry: ChainRegistry,
) : ParachainStakingUpdater<MetaAccount> {

    override suspend fun listenForUpdates(
        storageSubscriptionBuilder: SharedRequestsBuilder,
        scopeValue: MetaAccount
    ): Flow<Updater.SideEffect> {
        val account = scopeValue
        val (chain, asset) = stakingSharedState.chainAndAsset()
        val runtime = chainRegistry.getRuntime(chain.id)

        val accountId = account.accountIdIn(chain) ?: return emptyFlow()

        val storage = runtime.metadata.parachainStaking().storage("DelegatorState")
        val key = storage.storageKey(runtime, accountId)

        return storageSubscriptionBuilder.subscribe(key).map {
            val dynamicInstance = storage.decodeValue(it.value, runtime)
            val delegationState = bindDelegatorState(dynamicInstance, accountId, chain, asset)

            fetchUnbondings(delegationState, it.block)?.let { unbondings ->
                storageCache.insert(unbondings, chain.id)
            }
        }
            .noSideAffects()
    }

    private suspend fun fetchUnbondings(
        delegatorState: DelegatorState,
        blockHash: String
    ): Map<String, String?>? = when (delegatorState) {
        is DelegatorState.Delegator -> {
            val delegatorIdsArgs = delegatorState.delegations.map { it.owner }.wrapSingleArgumentKeys()

            remoteStorageDataSource.query(chainId = delegatorState.chain.id, at = blockHash) {
                runtime.metadata.parachainStaking().storage("DelegationScheduledRequests").entriesRaw(delegatorIdsArgs)
            }
        }
        is DelegatorState.None -> null
    }
}
