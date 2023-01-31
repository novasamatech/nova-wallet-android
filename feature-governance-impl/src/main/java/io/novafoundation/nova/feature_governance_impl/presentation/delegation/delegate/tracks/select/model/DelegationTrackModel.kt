package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.tracks.select.model

import io.novafoundation.nova.feature_governance_impl.presentation.common.models.TrackModel

data class DelegationTrackModel(
    val details: TrackModel,
    val isSelected: Boolean
)
