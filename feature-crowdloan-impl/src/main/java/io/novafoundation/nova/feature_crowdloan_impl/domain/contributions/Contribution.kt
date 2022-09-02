package io.novafoundation.nova.feature_crowdloan_impl.domain.contributions

import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novafoundation.nova.common.utils.formatting.TimerValue
import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.FundInfo
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ParachainMetadata
import java.math.BigInteger

class Contribution(
    val amount: BigInteger,
    val paraId: ParaId,
    val fundInfo: FundInfo,
    val sourceName: String?,
    val returnsIn: TimerValue,
    val parachainMetadata: ParachainMetadata?,
)

class ContributionsWithTotalAmount(val totalContributed: BigInteger, val contributions: List<Contribution>)
