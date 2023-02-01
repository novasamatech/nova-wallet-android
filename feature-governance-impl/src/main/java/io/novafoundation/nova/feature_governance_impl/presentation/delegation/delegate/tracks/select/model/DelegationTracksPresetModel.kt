package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.tracks.select.model

import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.create.chooseTrack.model.TrackPreset

data class DelegationTracksPresetModel(
    val label: String,
    val trackPresetModels: TrackPreset.Type
)
