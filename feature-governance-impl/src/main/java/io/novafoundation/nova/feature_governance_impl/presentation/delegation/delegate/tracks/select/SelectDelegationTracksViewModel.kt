package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.tracks.select

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.toggle
import io.novafoundation.nova.common.utils.withSafeLoading
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
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.removeVotes.RemoveVotesPayload
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.create.chooseAmount.NewDelegationChooseAmountPayload
import io.novafoundation.nova.feature_governance_impl.presentation.track.TrackFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.track.TrackModel
import io.novafoundation.nova.runtime.state.chainAsset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class UnavailableTracksModel(val alreadyVoted: List<TrackModel>, val alreadyDelegated: List<TrackModel>)

class SelectDelegationTracksViewModel(
    private val newDelegationChooseTrackInteractor: NewDelegationChooseTrackInteractor,
    private val trackFormatter: TrackFormatter,
    private val governanceSharedState: GovernanceSharedState,
    private val resourceManager: ResourceManager,
    private val router: GovernanceRouter,
    private val payload: SelectDelegationTracksPayload
) : BaseViewModel() {

    private val chooseTrackDataFlow = newDelegationChooseTrackInteractor.observeChooseTrackData()
        .shareInBackground()

    private var selectedTracksFlow = MutableStateFlow(setOf<TrackId>())

    private val trackPresetsFlow = chooseTrackDataFlow.map { it.presets }

    private val availableTrackFlow = chooseTrackDataFlow.map { it.trackPartition.available }

    private val _showRemoveVotesSuggestion = MutableLiveData<Event<Int>>()
    val showRemoveVotesSuggestion: LiveData<Event<Int>> = _showRemoveVotesSuggestion

    private val _showUnavailableTracksEvent = MutableLiveData<Event<UnavailableTracksModel>>()
    val showUnavailableTracksEvent: LiveData<Event<UnavailableTracksModel>> = _showUnavailableTracksEvent

    val trackPresetsModels = trackPresetsFlow
        .map { mapTrackPresets(it) }
        .shareInBackground()

    val availableTrackModels = combine(availableTrackFlow, selectedTracksFlow) { tracks, selectedTracks ->
        mapTracksToModel(tracks, selectedTracks)
    }.withSafeLoading()
        .shareInBackground()

    val showUnavailableTracksButton = chooseTrackDataFlow
        .map { it.trackPartition.hasUnavailableTracks() }
        .shareInBackground()

    val buttonState = selectedTracksFlow
        .map {
            if (it.isEmpty()) {
                DescriptiveButtonState.Disabled(resourceManager.getString(R.string.delegation_tracks_disabled_apply_button_text))
            } else {
                DescriptiveButtonState.Enabled(resourceManager.getString(R.string.common_continue))
            }
        }
        .shareInBackground()

    init {
        checkExistingVotes()
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
        launch {
            val tracksPartition = chooseTrackDataFlow.first().trackPartition
            val alreadyVotedModels = mapTracks(tracksPartition.alreadyVoted)
            val alreadyDelegatedModels = mapTracks(tracksPartition.alreadyDelegated)
            _showUnavailableTracksEvent.value = Event(UnavailableTracksModel(alreadyVotedModels, alreadyDelegatedModels))
        }
    }

    fun nextClicked() = launch {
        val selectedTrackIds = selectedTracksFlow.value
        val payload = NewDelegationChooseAmountPayload(
            delegate = payload.delegateId,
            trackIdsRaw = selectedTrackIds.map(TrackId::value)
        )
        router.openNewDelegationChooseAmount(payload)
    }

    fun openRemoveVotesScreen() {
        launch {
            val chooseTrackData = chooseTrackDataFlow.first()
            val tracksIds = chooseTrackData.trackPartition.alreadyVoted
                .map { it.id.value }
            val payload = RemoveVotesPayload(tracksIds)
            router.openRemoveVotes(payload)
        }
    }

    fun presetClicked(position: Int) {
        launch {
            val selectedPreset = trackPresetsFlow.first()[position]
            selectedTracksFlow.value = selectedPreset.trackIds.toHashSet()
        }
    }

    private fun checkExistingVotes() {
        launch {
            val chooseTrackData = chooseTrackDataFlow.first()
            val alreadyVoted = chooseTrackData.trackPartition.alreadyVoted
            if (alreadyVoted.isNotEmpty()) {
                _showRemoveVotesSuggestion.value = Event(alreadyVoted.size)
            }
        }
    }

    private fun mapTrackPresets(trackPresets: List<TrackPreset>): List<DelegationTracksPresetModel> {
        return trackPresets.map {
            DelegationTracksPresetModel(
                label = mapPresetTypeToButtonName(it.type),
                trackPresetModels = it.type
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

    private suspend fun mapTracksToModel(tracks: List<Track>, selectedTracks: Set<TrackId>): List<DelegationTrackModel> {
        val asset = governanceSharedState.chainAsset()

        return tracks.map {
            val trackModel = trackFormatter.formatTrack(it, asset)
            DelegationTrackModel(
                trackModel,
                isSelected = it.id in selectedTracks
            )
        }
    }

    private suspend fun mapTracks(tracks: List<Track>): List<TrackModel> {
        val asset = governanceSharedState.chainAsset()
        return tracks.map { trackFormatter.formatTrack(it, asset) }
    }
}
