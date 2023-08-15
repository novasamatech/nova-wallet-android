package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.controller

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.system
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.core_db.model.AccountStakingLocal
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.scope.AccountStakingScope
import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.feature_wallet_api.data.cache.bindAccountInfoOrDefault
import io.novafoundation.nova.feature_wallet_api.data.cache.updateAsset
import io.novafoundation.nova.runtime.ext.disabled
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.state.chainAndAsset
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach

class AccountControllerBalanceUpdater(
    override val scope: AccountStakingScope,
    private val sharedState: StakingSharedState,
    private val chainRegistry: ChainRegistry,
    private val assetCache: AssetCache,
) : Updater<AccountStakingLocal> {

    override val requiredModules: List<String> = listOf(Modules.SYSTEM, Modules.STAKING)

    override suspend fun listenForUpdates(
        storageSubscriptionBuilder: SharedRequestsBuilder,
        scopeValue: AccountStakingLocal
    ): Flow<Updater.SideEffect> {
        val (chain, chainAsset) = sharedState.chainAndAsset()
        if (chainAsset.disabled) return emptyFlow()

        val runtime = chainRegistry.getRuntime(chain.id)

        val accountStaking = scopeValue
        val stakingAccessInfo = accountStaking.stakingAccessInfo ?: return emptyFlow()

        val controllerId = stakingAccessInfo.controllerId
        val stashId = stakingAccessInfo.stashId
        val accountId = accountStaking.accountId

        if (controllerId.contentEquals(stashId)) {
            // balance is already observed, no need to do it twice
            return emptyFlow()
        }

        val companionAccountId = when {
            accountId.contentEquals(controllerId) -> stashId
            accountId.contentEquals(stashId) -> controllerId
            else -> throw IllegalArgumentException()
        }

        val key = runtime.metadata.system().storage("Account").storageKey(runtime, companionAccountId)

        return storageSubscriptionBuilder.subscribe(key)
            .onEach { change ->
                val newAccountInfo = bindAccountInfoOrDefault(change.value, runtime)

                assetCache.updateAsset(companionAccountId, chainAsset, newAccountInfo)
            }
            .flowOn(Dispatchers.IO)
            .noSideAffects()
    }
}
