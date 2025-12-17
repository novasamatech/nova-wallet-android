package io.novafoundation.nova.feature_push_notifications.data.settings.model.governance

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_push_notifications.domain.model.PushSettings

class GovernanceStateCacheV1(
    val newReferendaEnabled: Boolean,
    val referendumUpdateEnabled: Boolean,
    val govMyDelegateVotedEnabled: Boolean,
    val tracks: Set<TrackId>
)

fun GovernanceStateCacheV1.toDomain(): PushSettings.GovernanceState {
    return PushSettings.GovernanceState(
        newReferendaEnabled = newReferendaEnabled,
        referendumUpdateEnabled = referendumUpdateEnabled,
        govMyDelegateVotedEnabled = govMyDelegateVotedEnabled,
        tracks = tracks
    )
}
