package io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.common.chooseTrack.model

data class ChooseTrackData(
    val trackPartition: TrackPartition,
    val presets: List<TrackPreset>
)
