package io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.model

import io.novafoundation.nova.feature_governance_impl.presentation.track.TrackModel

data class DelegationTrackModel(
    val details: TrackModel,
    val isSelected: Boolean
)
