package io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.base.model

import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.common.chooseTrack.model.TrackPreset

data class DelegationTracksPresetModel(
    val label: String,
    val trackPresetModels: TrackPreset.Type
)
