package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.tracks.select

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.toggle
import io.novafoundation.nova.common.utils.withLoadingResult
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.create.chooseTrack.NewDelegationChooseTrackInteractor
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.create.chooseTrack.model.TrackPreset
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.create.chooseTrack.model.hasUnavailableTracks
import io.novafoundation.nova.feature_governance_api.domain.track.Track
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.tracks.select.model.DelegationTrackModel
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.tracks.select.model.DelegationTracksPresetModel
import io.novafoundation.nova.feature_governance_impl.presentation.track.TrackFormatter
import io.novafoundation.nova.runtime.state.chainAsset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SelectDelegationTracksViewModel(
    private val newDelegationChooseTrackInteractor: NewDelegationChooseTrackInteractor,
    private val trackFormatter: TrackFormatter,
    private val governanceSharedState: GovernanceSharedState,
    private val resourceManager: ResourceManager,
    private val router: GovernanceRouter,
    private val payload: SelectDelegationTracksPayload
) : BaseViewModel() {

    private val chooseTrackDataFlow = newDelegationChooseTrackInteractor.observeChooseTrackData()

    private var selectedTracksFlow = MutableStateFlow(setOf<TrackId>())

    private val trackPresetsFlow = chooseTrackDataFlow.map { it.presets }

    private val availableTrackFlow = chooseTrackDataFlow.map { it.trackPartition.available }

    val trackPresetsModels = trackPresetsFlow
        .map { mapTrackPresets(it) }
        .shareInBackground()

    val availableTrackModels = combine(availableTrackFlow, selectedTracksFlow) { tracks, selectedTracks ->
        mapTracksToModel(tracks, selectedTracks.toList())
    }.withLoadingResult()
        .shareInBackground()

    val showUnavailableTracksButton = chooseTrackDataFlow
        .map { it.trackPartition.hasUnavailableTracks() }
        .shareInBackground()

    val isButtonEnabled = selectedTracksFlow
        .map { it.isNotEmpty() }
        .shareInBackground()

    init {
        launch {
            val chooseTrackData = chooseTrackDataFlow.first()
            if (chooseTrackData.trackPartition.alreadyVoted.isNotEmpty()) {
                // TODO open Remove votes bottom sheet
            }
        }
    }

    fun backClicked() {
        router.back()
    }

    fun trackClicked(position: Int) {
        launch {
            val track = availableTrackFlow.first()[position]
            val selectedTracks = selectedTracksFlow.value
            selectedTracksFlow.value = selectedTracks.toggle(track.id)
        }
    }

    fun unavailableTracksClicked() {
    }

    fun openSetupConviction() {
    }

    fun presetClicked(position: Int) {
        launch {
            val selectedPreset = trackPresetsFlow.first()[position]
            selectedTracksFlow.value = selectedPreset.trackIds.toHashSet()
        }
    }

    private fun mapTrackPresets(trackPresets: List<TrackPreset>): List<DelegationTracksPresetModel> {
        return trackPresets.map {
            DelegationTracksPresetModel(
                mapPresetTypeToButtonName(it.type),
                it.type
            )
        }
    }

    private fun mapPresetTypeToButtonName(trackPresetType: TrackPreset.Type): String {
        return when (trackPresetType) {
            TrackPreset.Type.ALL -> resourceManager.getString(R.string.delegation_tracks_all_preset)
            TrackPreset.Type.FELLOWSHIP -> resourceManager.getString(R.string.delegation_tracks_fellowship_preset)
            TrackPreset.Type.TREASURY -> resourceManager.getString(R.string.delegation_tracks_treasury_preset)
            TrackPreset.Type.GOVERNANCE -> resourceManager.getString(R.string.delegation_tracks_governance_preset)
        }
    }

    private suspend fun mapTracksToModel(tracks: List<Track>, selectedTracks: List<TrackId>): List<DelegationTrackModel> {
        val asset = governanceSharedState.chainAsset()
        return tracks.map {
            val trackModel = trackFormatter.formatTrack(it, asset)
            DelegationTrackModel(
                trackModel,
                isSelected = it.id in selectedTracks
            )
        }
    }
}
