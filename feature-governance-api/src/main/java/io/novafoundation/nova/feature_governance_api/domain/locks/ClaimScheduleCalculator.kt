package io.novafoundation.nova.feature_governance_api.domain.locks

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

interface ClaimScheduleCalculator {

    fun totalGovernanceLock(): Balance

    fun maxConvictionEndOf(vote: AccountVote, referendumId: ReferendumId): BlockNumber

    fun estimateClaimSchedule(): ClaimSchedule
}
