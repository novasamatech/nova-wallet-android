package io.novafoundation.nova.feature_crowdloan_api.domain.contributions

import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novafoundation.nova.common.utils.formatting.TimerValue
import io.novafoundation.nova.core_db.model.ContributionLocal
import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.FundInfo
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ParachainMetadata
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger


open class Contribution(
    val chain: Chain,
    val amountInPlanks: BigInteger,
    val paraId: ParaId,
    val sourceName: String?,
    val returnsIn: TimerValue,
    val type: Type,
) {

    enum class Type {
        DIRECT, ACALA, PARALLEL
    }
}

class ContributionWithMetadata(
    chain: Chain,
    amount: BigInteger,
    paraId: ParaId,
    sourceName: String?,
    returnsIn: TimerValue,
    type: Type,
    val fundInfo: FundInfo,
    val parachainMetadata: ParachainMetadata?,
) : Contribution(chain, amount, paraId, sourceName, returnsIn, type)

class ContributionsWithTotalAmount(val totalContributed: BigInteger, val contributions: List<ContributionWithMetadata>)

fun mapContributionToLocal(metaId: Long, contribution: Contribution): ContributionLocal {
    return ContributionLocal(
        metaId,
        contribution.chain.id,
        contribution.paraId,
        contribution.amountInPlanks,
        contribution.sourceName,
        contribution.returnsIn.millis,
        mapContributionTypeToLocal(contribution.type)
    )
}

fun mapContributionTypeToLocal(contributionType: Contribution.Type): ContributionLocal.Type {
    return when (contributionType) {
        Contribution.Type.DIRECT -> ContributionLocal.Type.DIRECT
        Contribution.Type.ACALA -> ContributionLocal.Type.ACALA
        Contribution.Type.PARALLEL -> ContributionLocal.Type.PARALLEL
    }
}

fun mapContributionFromLocal(
    contribution: ContributionLocal,
    chain: Chain,
): Contribution {
    return Contribution(
        chain,
        contribution.amountInPlanks,
        contribution.paraId,
        contribution.sourceName,
        TimerValue.fromCurrentTime(contribution.returnsIn),
        mapContributionTypeFromLocal(contribution.type),
    )
}

fun mapContributionTypeFromLocal(contributionType: ContributionLocal.Type): Contribution.Type {
    return when (contributionType) {
        ContributionLocal.Type.DIRECT -> Contribution.Type.DIRECT
        ContributionLocal.Type.ACALA -> Contribution.Type.ACALA
        ContributionLocal.Type.PARALLEL -> Contribution.Type.PARALLEL
    }
}
