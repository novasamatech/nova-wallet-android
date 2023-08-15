package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters

import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.ALEPH_ZERO
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.NOMINATION_POOLS
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.PARACHAIN
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.RELAYCHAIN
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.RELAYCHAIN_AURA
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.TURING
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.UNSUPPORTED

class StakingUpdaters(
    private val relaychainUpdaters: List<Updater>,
    private val parachainUpdaters: List<Updater>,
    private val commonUpdaters: List<Updater>,
    private val turingExtraUpdaters: List<Updater>,
    private val nominationPoolsUpdaters: List<Updater>,
) {

    fun getUpdaters(stakingType: StakingType): List<Updater> {
        return commonUpdaters + getUpdatersByType(stakingType)
    }

    fun getUpdaters(stakingTypes: List<StakingType>): List<Updater> {
        return commonUpdaters + stakingTypes.flatMap { getUpdatersByType(it) }.toSet()
    }

    private fun getUpdatersByType(stakingType: StakingType): List<Updater> {
        return when (stakingType) {
            UNSUPPORTED -> emptyList()
            RELAYCHAIN, RELAYCHAIN_AURA, ALEPH_ZERO -> relaychainUpdaters
            PARACHAIN -> parachainUpdaters
            TURING -> parachainUpdaters + turingExtraUpdaters
            NOMINATION_POOLS -> nominationPoolsUpdaters
        }
    }
}
