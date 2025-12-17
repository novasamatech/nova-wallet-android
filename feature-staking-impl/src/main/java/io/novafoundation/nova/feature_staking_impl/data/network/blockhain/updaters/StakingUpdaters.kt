package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters

import io.novafoundation.nova.common.utils.MultiMap
import io.novafoundation.nova.common.utils.buildMultiMap
import io.novafoundation.nova.common.utils.putAll
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.ALEPH_ZERO
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.MYTHOS
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.NOMINATION_POOLS
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.PARACHAIN
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.RELAYCHAIN
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.RELAYCHAIN_AURA
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.TURING
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.UNSUPPORTED
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.network.updaters.multiChain.SharedStateBasedUpdater
import io.novafoundation.nova.runtime.network.updaters.multiChain.groupBySyncingChain

class StakingUpdaters(
    private val relaychainUpdaters: Group,
    private val parachainUpdaters: Group,
    private val commonUpdaters: Group,
    private val turingExtraUpdaters: Group,
    private val nominationPoolsUpdaters: Group,
    private val mythosUpdaters: Group,
) {

    class Group(val updaters: List<SharedStateBasedUpdater<*>>) {
        constructor(vararg updaters: SharedStateBasedUpdater<*>) : this(updaters.toList())
    }

    fun getUpdaters(stakingChain: Chain, stakingType: StakingType): MultiMap<ChainId, Updater<*>> {
        return buildMultiMap {
            putAll(getCommonUpdaters(stakingChain))
            putAll(getUpdatersByType(stakingChain, stakingType))
        }
    }

    fun getUpdaters(stakingChain: Chain, stakingTypes: List<StakingType>): MultiMap<ChainId, Updater<*>> {
        return buildMultiMap {
            putAll(getCommonUpdaters(stakingChain))

            stakingTypes.forEach {
                putAll(getUpdatersByType(stakingChain, it))
            }
        }
    }

    private fun getCommonUpdaters(stakingChain: Chain): MultiMap<ChainId, SharedStateBasedUpdater<*>> {
        return commonUpdaters.updaters.groupBySyncingChain(stakingChain)
    }

    private fun getUpdatersByType(stakingChain: Chain, stakingType: StakingType): MultiMap<ChainId, SharedStateBasedUpdater<*>> {
        val byTypeUpdaters = when (stakingType) {
            RELAYCHAIN, RELAYCHAIN_AURA, ALEPH_ZERO -> relaychainUpdaters.updaters
            PARACHAIN -> parachainUpdaters.updaters
            TURING -> parachainUpdaters.updaters + turingExtraUpdaters.updaters
            NOMINATION_POOLS -> nominationPoolsUpdaters.updaters
            MYTHOS -> mythosUpdaters.updaters
            UNSUPPORTED -> emptyList()
        }

        return byTypeUpdaters.groupBySyncingChain(stakingChain)
    }
}
