package io.novafoundation.nova.feature_crowdloan_api.domain.contributions

import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novafoundation.nova.common.utils.formatting.TimerValue
import io.novafoundation.nova.core_db.model.ContributionLocal
import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.FundInfo
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ParachainMetadata
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

class Contribution(
    val chain: Chain,
    val amountInPlanks: BigInteger,
    val paraId: ParaId,
    val sourceId: String,
) {

    companion object {
        const val DIRECT_SOURCE_ID = "direct"
    }
}

class ContributionMetadata(
    val returnsIn: TimerValue,
    val fundInfo: FundInfo,
    val parachainMetadata: ParachainMetadata?,
)

class ContributionWithMetadata(
    val contribution: Contribution,
    val metadata: ContributionMetadata
)

class ContributionsWithTotalAmount(val totalContributed: BigInteger, val contributions: List<ContributionWithMetadata>)

fun mapContributionToLocal(metaId: Long, contribution: Contribution): ContributionLocal {
    return ContributionLocal(
        metaId,
        contribution.chain.id,
        contribution.paraId,
        contribution.amountInPlanks,
        contribution.sourceId,
    )
}

fun mapContributionFromLocal(
    contribution: ContributionLocal,
    chain: Chain,
): Contribution {
    return Contribution(
        chain,
        contribution.amountInPlanks,
        contribution.paraId,
        contribution.sourceId,
    )
}
