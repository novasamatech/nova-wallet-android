package io.novafoundation.nova.feature_xcm_api.multiLocation

import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novafoundation.nova.common.utils.collectionIndexOrNull
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation.Junction

/**
 * A location expressed from the root of the local consensus system (the relay chain): chains are `Here` (relay)
 * or `Parachain(id)`, assets append their pallet/index junctions.
 *
 * Invariant: a location starting with a [Junction.GlobalConsensus] junction is rooted in another consensus system
 * (e.g. Snowbridge assets under `GlobalConsensus(Ethereum)`); locations of local chains never start with one.
 * Only the dynamic XCM config mapper produces such locations.
 */
class AbsoluteMultiLocation(
    interior: Interior,
) : MultiLocation(interior) {

    companion object;

    constructor(vararg junctions: Junction) : this(junctions.toList().toInterior())

    fun toRelative(): RelativeMultiLocation {
        return RelativeMultiLocation(parents = 0, interior = interior)
    }

    /**
     * Reanchor given location to a point of view of given `pov` location, which must be a location inside the
     * local consensus system (a chain location).
     *
     * A location rooted in another consensus system (leading [Junction.GlobalConsensus]) has no common ancestor
     * with `pov`: the result goes up to the relay (one hop per `pov` junction) plus one more hop to the universal
     * level and keeps the full path, e.g. Snowbridge ETH from a parachain is `parents = 2, X1(GlobalConsensus(Ethereum))`.
     *
     * Otherwise, basic algorithm idea:
     * We find the last common ancestor and consider the target location to be "up to ancestor and down to self":
     *  1. Find last common ancestor between `this` and `pov`
     *  2. Use all junctions after common ancestor as result junctions
     *  3. Use difference between len(target.junctions) and common_ancestor_idx
     *   to determine how many "up" hops are needed to reach common ancestor
     */
    fun fromPointOfViewOf(pov: AbsoluteMultiLocation): RelativeMultiLocation {
        require(!pov.isForeignConsensus()) { "Point of view must be a location inside the local consensus: $pov" }

        if (isForeignConsensus()) {
            return RelativeMultiLocation(parents = pov.junctions.size + 1, interior = interior)
        }

        val lastCommonIndex = findLastCommonJunctionIndex(pov)
        val firstDistinctIndex = lastCommonIndex?.let { it + 1 } ?: 0

        val parents = pov.junctions.size - firstDistinctIndex
        val junctions = junctions.drop(firstDistinctIndex)

        return RelativeMultiLocation(parents, junctions.toInterior())
    }

    private fun isForeignConsensus(): Boolean = junctions.firstOrNull() is Junction.GlobalConsensus

    private fun findLastCommonJunctionIndex(other: AbsoluteMultiLocation): Int? {
        return junctions.zip(other.junctions).indexOfLast { (selfJunction, otherJunction) ->
            selfJunction == otherJunction
        }.collectionIndexOrNull()
    }
}

fun AbsoluteMultiLocation.Companion.chainLocation(parachainId: ParaId?): AbsoluteMultiLocation {
    return listOfNotNull(parachainId?.let(MultiLocation.Junction::ParachainId)).asLocation()
}
