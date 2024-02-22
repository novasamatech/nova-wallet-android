package io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.common.chooseTrack.model

data class ChooseTrackData(
    val trackPartition: TrackPartition,
    val presets: List<TrackPreset>
) {

    companion object {

        fun empty(): ChooseTrackData {
            return ChooseTrackData(
                TrackPartition(emptySet(), emptyList(), emptyList(), emptyList()),
                emptyList()
            )
        }
    }
}

