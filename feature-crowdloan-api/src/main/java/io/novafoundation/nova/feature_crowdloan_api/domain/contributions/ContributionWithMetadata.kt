package io.novafoundation.nova.feature_crowdloan_api.domain.contributions

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novafoundation.nova.common.utils.formatting.TimerValue
import io.novafoundation.nova.core_db.model.ContributionLocal
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ParachainMetadata
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.util.BlockDurationEstimator
import io.novafoundation.nova.runtime.util.timerUntil
import java.math.BigInteger

class Contribution(
    val chain: Chain,
    val asset: Chain.Asset,
    val amountInPlanks: BigInteger,
    val paraId: ParaId,
    val sourceId: String,
    val unlockBlock: BlockNumber,
    val leaseDepositor: AccountIdKey,
) {

    companion object {
        const val DIRECT_SOURCE_ID = "direct"
        const val LIQUID_SOURCE_ID = "liquid"
        const val PARALLEL_SOURCE_ID = "parallel"
    }
}

class ContributionMetadata(
    val claimStatus: ContributionClaimStatus,
    val parachainMetadata: ParachainMetadata?,
)

sealed class ContributionClaimStatus {

    object Claimable : ContributionClaimStatus()

    class ReturnsIn(val timer: TimerValue): ContributionClaimStatus()
}


class ContributionWithMetadata(
    val contribution: Contribution,
    val metadata: ContributionMetadata
)

fun BlockDurationEstimator.claimStatusOf(contribution: Contribution): ContributionClaimStatus {
    return if (contribution.unlockBlock > currentBlock) {
        ContributionClaimStatus.ReturnsIn(timerUntil(contribution.unlockBlock))
    } else {
        ContributionClaimStatus.Claimable
    }
}

class ContributionsWithTotalAmount<T>(val totalContributed: BigInteger, val contributions: List<T>) {
    companion object {
        fun <T> empty(): ContributionsWithTotalAmount<T> {
            return ContributionsWithTotalAmount(BigInteger.ZERO, emptyList())
        }

        fun fromContributions(contributions: List<Contribution>): ContributionsWithTotalAmount<Contribution> {
            return ContributionsWithTotalAmount(
                totalContributed = contributions.sumOf { it.amountInPlanks },
                contributions = contributions
            )
        }
    }
}

fun mapContributionToLocal(metaId: Long, contribution: Contribution): ContributionLocal {
    return ContributionLocal(
        metaId,
        contribution.chain.id,
        contribution.asset.id,
        contribution.paraId,
        contribution.amountInPlanks,
        contribution.sourceId,
        unlockBlock = contribution.unlockBlock,
        leaseDepositor = contribution.leaseDepositor.value
    )
}

fun mapContributionFromLocal(
    contribution: ContributionLocal,
    chain: Chain,
): Contribution {
    return Contribution(
        chain,
        chain.utilityAsset,
        contribution.amountInPlanks,
        contribution.paraId,
        contribution.sourceId,
        unlockBlock = contribution.unlockBlock,
        leaseDepositor = contribution.leaseDepositor.intoKey()
    )
}
