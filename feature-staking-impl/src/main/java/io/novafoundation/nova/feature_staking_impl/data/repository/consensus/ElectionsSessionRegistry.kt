package io.novafoundation.nova.feature_staking_impl.data.repository.consensus

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.ALEPH_ZERO
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.PARACHAIN
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.RELAYCHAIN
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.RELAYCHAIN_AURA
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.UNSUPPORTED

interface ElectionsSessionRegistry {

    fun electionsSessionFor(chainAsset: Chain.Asset): ElectionsSession
}

class RealElectionsSessionRegistry(
    private val babeSession: BabeSession,
    private val auraSession: AuraSession
) : ElectionsSessionRegistry {

    override fun electionsSessionFor(chainAsset: Chain.Asset): ElectionsSession {
        return when (chainAsset.staking) {
            RELAYCHAIN -> babeSession
            RELAYCHAIN_AURA, ALEPH_ZERO -> auraSession
            UNSUPPORTED, PARACHAIN -> throw IllegalArgumentException("Unsupported staking type in RealStakingSessionRegistry")
        }
    }
}
