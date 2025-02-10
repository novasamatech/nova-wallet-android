package io.novafoundation.nova.feature_staking_impl.data.mythos.updaters

import io.novafoundation.nova.common.utils.RuntimeContext
import io.novafoundation.nova.common.utils.metadata
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core.updater.GlobalScope
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.api.collatorRewardPercentage
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.api.collatorStaking
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.api.extraReward
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.api.minStake
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.network.updaters.SingleStorageKeyUpdater
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot

class MythosCollatorRewardPercentageUpdater(
    stakingSharedState: StakingSharedState,
    chainRegistry: ChainRegistry,
    storageCache: StorageCache
) : SingleStorageKeyUpdater<Unit>(GlobalScope, stakingSharedState, chainRegistry, storageCache), Updater<Unit> {

    override suspend fun storageKey(runtime: RuntimeSnapshot, scopeValue: Unit): String {
        return with(RuntimeContext(runtime)) {
            metadata.collatorStaking.collatorRewardPercentage.storageKey()
        }
    }
}
