package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters

import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.*

class StakingUpdaters(
    private val relaychainUpdaters: Group,
    private val parachainUpdaters: Group,
    private val commonUpdaters: Group,
    private val turingExtraUpdaters: Group,
    private val nominationPoolsUpdaters: Group,
    private val mythosUpdaters: Group,
) {

    class Group(val updaters: List<Updater<*>>) {
        constructor(vararg updaters: Updater<*>) : this(updaters.toList())
    }

    fun getUpdaters(stakingType: StakingType): List<Updater<*>> {
        return commonUpdaters.updaters + getUpdatersByType(stakingType)
    }

    fun getUpdaters(stakingTypes: List<StakingType>): List<Updater<*>> {
        return commonUpdaters.updaters + stakingTypes.flatMapTo(mutableSetOf(), ::getUpdatersByType)
    }

    private fun getUpdatersByType(stakingType: StakingType): List<Updater<*>> {
        return when (stakingType) {
            UNSUPPORTED -> emptyList()
            RELAYCHAIN, RELAYCHAIN_AURA, ALEPH_ZERO -> relaychainUpdaters.updaters
            PARACHAIN -> parachainUpdaters.updaters
            TURING -> parachainUpdaters.updaters + turingExtraUpdaters.updaters
            NOMINATION_POOLS -> nominationPoolsUpdaters.updaters
            MYTHOS -> mythosUpdaters.updaters
        }
    }
}
