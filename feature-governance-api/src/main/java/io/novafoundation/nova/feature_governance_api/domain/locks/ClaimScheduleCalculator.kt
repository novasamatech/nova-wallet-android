package io.novafoundation.nova.feature_governance_api.domain.locks

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId

interface ClaimScheduleCalculator {

    fun maxConvictionEndOf(vote: AccountVote, referendumId: ReferendumId): BlockNumber

    fun estimateClaimSchedule(): ClaimSchedule
}
