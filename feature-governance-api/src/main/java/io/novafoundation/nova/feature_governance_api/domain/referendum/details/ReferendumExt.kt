package io.novafoundation.nova.feature_governance_api.domain.referendum.details

import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumStatus
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumVote

fun ReferendumDetails.isUserDelegatedVote() = userVote is ReferendumVote.UserDelegated

fun ReferendumDetails.isUserDirectVote() = userVote is ReferendumVote.UserDirect

fun ReferendumDetails.noVote() = userVote == null

fun ReferendumStatus.isOngoing(): Boolean {
    return this is ReferendumStatus.Ongoing
}

fun ReferendumDetails.isFinished() = !timeline.currentStatus.isOngoing()
