package io.novafoundation.nova.feature_xcm_impl.converter.chain

import io.novafoundation.nova.feature_xcm_api.converter.chain.ChainMultiLocationConverter
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation.Junction.ParachainId
import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.junctions
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getChainOrNull
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

    override suspend fun toChain(multiLocation: RelativeMultiLocation): Chain? {
        // This is not a child parachain from relay point
        if (multiLocation.parents != 0) return null

        val junctions = multiLocation.junctions
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
