package io.novafoundation.nova.runtime.multiNetwork.multiLocation.converter.chain

import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getChainOrNull
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.MultiLocation
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.MultiLocation.Junction.ParachainId
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.junctionList
import java.math.BigInteger

internal class ChildParachainLocationConverter(
    private val relayChain: Chain,
    private val chainRegistry: ChainRegistry
) : ChainMultiLocationConverter {

    private val parachainIdToChainIdByRelay = mapOf(
        Chain.Geneses.POLKADOT to mapOf(
            1000 to Chain.Geneses.POLKADOT_ASSET_HUB
        )
    )

    override suspend fun toChain(multiLocation: MultiLocation): Chain? {
        // This is not a child parachain from relay point
        if (multiLocation.parents != BigInteger.ZERO) return null

        val junctions = multiLocation.interior.junctionList
        // Child parachain has only 1 ParachainId junction
        if (junctions.size != 1) return null
        val parachainId = junctions.single() as? ParachainId ?: return null

        val parachainChainId = getParachainChainId(parachainId.id) ?: return null

        return chainRegistry.getChainOrNull(parachainChainId)
    }

    private fun getParachainChainId(parachainId: BigInteger): ChainId? {
        return parachainIdToChainIdByRelay[relayChain.id]?.get(parachainId.toInt())
    }
}
