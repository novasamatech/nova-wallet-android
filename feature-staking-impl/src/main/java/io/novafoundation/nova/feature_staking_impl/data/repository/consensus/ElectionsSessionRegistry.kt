package io.novafoundation.nova.feature_staking_impl.data.repository.consensus

import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.stakingType
import io.novafoundation.nova.feature_staking_impl.data.unwrapNominationPools
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.ALEPH_ZERO
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.MYTHOS
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.RELAYCHAIN
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.RELAYCHAIN_AURA

interface ElectionsSessionRegistry {

    fun electionsSessionFor(stakingOption: StakingOption): ElectionsSession
}

class RealElectionsSessionRegistry(
    private val babeSession: BabeSession,
    private val auraSession: AuraSession
) : ElectionsSessionRegistry {

    override fun electionsSessionFor(stakingOption: StakingOption): ElectionsSession {
        return electionsFor(stakingOption.unwrapNominationPools().stakingType)
    }

    private fun electionsFor(stakingType: Chain.Asset.StakingType): ElectionsSession {
        return when (stakingType) {
            RELAYCHAIN -> babeSession
            RELAYCHAIN_AURA, ALEPH_ZERO, MYTHOS -> auraSession
            else -> throw IllegalArgumentException("Unsupported staking type in RealStakingSessionRegistry")
        }
    }
}
