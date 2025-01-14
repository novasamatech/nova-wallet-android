package io.novafoundation.nova.feature_xcm_api.multiLocation

import io.novafoundation.nova.common.utils.collectionIndexOrNull

class AbsoluteMultiLocation(
    interior: Interior,
) : MultiLocation(interior) {

    fun toRelative(): RelativeMultiLocation {
        return RelativeMultiLocation(parents = 0, interior = interior)
    }

    /**
     * Reanchor given location to a point of view of given `target` location
     * Basic algorithm idea:
     * We find the last common ancestor and consider the target location to be "up to ancestor and down to self":
     *  1. Find last common ancestor between `self` and `target`
     *  2. Use all junctions after common ancestor as result junctions
     *  3. Use difference between len(target.junctions) and common_ancestor_idx
     *   to determine how many "up" hops are needed to reach common ancestor
     */
    fun reanchor(withRespectTo: AbsoluteMultiLocation): RelativeMultiLocation {
        val lastCommonIndex = findLastCommonJunctionIndex(withRespectTo)
        val firstDistinctIndex = lastCommonIndex?.let { it + 1 } ?: 0

        val parents = withRespectTo.junctions.size - firstDistinctIndex
        val junctions = junctions.drop(firstDistinctIndex)

        return RelativeMultiLocation(parents, junctions.toInterior())
    }

    private fun findLastCommonJunctionIndex(other: AbsoluteMultiLocation): Int? {
        return junctions.zip(other.junctions).indexOfLast { (selfJunction, otherJunction) ->
            selfJunction == otherJunction
        }.collectionIndexOrNull()
    }
}
