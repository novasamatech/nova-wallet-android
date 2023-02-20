package io.novafoundation.nova.feature_staking_impl.domain.bagList

import io.novafoundation.nova.feature_staking_impl.domain.model.BagListNode.Score

interface BagListLocator {

    fun bagBoundaries(userScore: Score): BagScoreBoundaries

    fun nextBagBoundaries(previous: BagScoreBoundaries): BagScoreBoundaries
}

typealias BagScoreBoundaries = ClosedRange<Score>

fun BagListLocator(thresholds: List<Score>): BagListLocator = RealBagListLocator(thresholds)

private class RealBagListLocator(private val thresholds: List<Score>) : BagListLocator {

    override fun bagBoundaries(userScore: Score): BagScoreBoundaries {
        val bagIndex = notionalBagIndexFor(userScore)

        return bagBoundariesAt(bagIndex)
    }

    override fun nextBagBoundaries(previous: BagScoreBoundaries): BagScoreBoundaries {
        val previousBagIndex = notionalBagIndexFor(previous.endInclusive)
        val nextBagIndex = (previousBagIndex + 1).coerceAtMost(thresholds.size - 1)

        return bagBoundariesAt(nextBagIndex)
    }

    private fun bagBoundariesAt(index: Int): BagScoreBoundaries {
        val bagUpper = thresholds[index]
        val bagLower = if (index > 0) thresholds[index - 1] else Score.zero()

        return bagLower..bagUpper
    }

    private fun notionalBagIndexFor(score: Score): Int {
        val index = thresholds.binarySearch(score)

        return if (index > 0) {
            index
        } else {
            val insertionPoint = (-index - 1) // convert from inverted insertion point

            insertionPoint
        }
    }
}
