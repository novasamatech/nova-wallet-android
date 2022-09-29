package io.novafoundation.nova.feature_crowdloan_impl.domain.contribute

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.FundInfo
import io.novafoundation.nova.feature_crowdloan_api.data.repository.LeasePeriodToBlocksConverter
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ParachainMetadata
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.Contribution
import io.novafoundation.nova.feature_crowdloan_impl.domain.main.Crowdloan
import java.math.BigInteger
import java.math.MathContext

fun mapFundInfoToCrowdloan(
    fundInfo: FundInfo,
    parachainMetadata: ParachainMetadata?,
    parachainId: BigInteger,
    currentBlockNumber: BlockNumber,
    expectedBlockTimeInMillis: BigInteger,
    leasePeriodToBlocksConverter: LeasePeriodToBlocksConverter,
    contribution: Contribution?,
    hasWonAuction: Boolean,
): Crowdloan {
    val leasePeriodInMillis = leasePeriodInMillis(leasePeriodToBlocksConverter, currentBlockNumber, fundInfo.lastSlot, expectedBlockTimeInMillis)

    val state = if (isCrowdloanActive(fundInfo, currentBlockNumber, leasePeriodToBlocksConverter, hasWonAuction)) {
        val remainingTime = expectedRemainingTime(currentBlockNumber, fundInfo.end, expectedBlockTimeInMillis)

        Crowdloan.State.Active(remainingTime)
    } else {
        Crowdloan.State.Finished
    }

    return Crowdloan(
        parachainMetadata = parachainMetadata,
        raisedFraction = fundInfo.raised.toBigDecimal().divide(fundInfo.cap.toBigDecimal(), MathContext.DECIMAL32),
        parachainId = parachainId,
        leasePeriodInMillis = leasePeriodInMillis,
        leasedUntilInMillis = System.currentTimeMillis() + leasePeriodInMillis,
        state = state,
        fundInfo = fundInfo,
        myContribution = contribution
    )
}

private fun isCrowdloanActive(
    fundInfo: FundInfo,
    currentBlockNumber: BigInteger,
    leasePeriodToBlocksConverter: LeasePeriodToBlocksConverter,
    hasWonAuction: Boolean,
): Boolean {
    return currentBlockNumber < fundInfo.end && // crowdloan is not ended
        // first slot is not yet passed
        leasePeriodToBlocksConverter.leaseIndexFromBlock(currentBlockNumber) <= fundInfo.firstSlot &&
        // cap is not reached
        fundInfo.raised < fundInfo.cap &&
        // crowdloan considered closed if parachain already won auction
        !hasWonAuction
}

fun leasePeriodInMillis(
    leasePeriodToBlocksConverter: LeasePeriodToBlocksConverter,
    currentBlockNumber: BigInteger,
    endingLeasePeriod: BigInteger,
    expectedBlockTimeInMillis: BigInteger
): Long {
    val unlockedAtPeriod = endingLeasePeriod + BigInteger.ONE // next period after end one
    val unlockedAtBlock = leasePeriodToBlocksConverter.startBlockFor(unlockedAtPeriod)

    return expectedRemainingTime(
        currentBlockNumber,
        unlockedAtBlock,
        expectedBlockTimeInMillis
    )
}

private fun expectedRemainingTime(
    currentBlock: BlockNumber,
    targetBlock: BlockNumber,
    expectedBlockTimeInMillis: BigInteger,
): Long {
    val blockDifference = targetBlock - currentBlock
    val expectedTimeDifference = blockDifference * expectedBlockTimeInMillis

    return expectedTimeDifference.toLong()
}
