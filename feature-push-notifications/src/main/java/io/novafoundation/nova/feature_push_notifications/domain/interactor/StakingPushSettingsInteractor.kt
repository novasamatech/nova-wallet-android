package io.novafoundation.nova.feature_push_notifications.domain.interactor

import io.novafoundation.nova.feature_staking_api.data.dashboard.common.stakingChainsFlow
import io.novafoundation.nova.runtime.ext.defaultComparator
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

interface StakingPushSettingsInteractor {

    fun stakingChainsFlow(): Flow<List<Chain>>
}

class RealStakingPushSettingsInteractor(
    private val chainRegistry: ChainRegistry
) : StakingPushSettingsInteractor {

    override fun stakingChainsFlow(): Flow<List<Chain>> {
        return chainRegistry.stakingChainsFlow()
            .map { chains ->
                chains.filter { it.pushSupport }
                    .sortedWith(Chain.defaultComparator())
            }
    }
}
