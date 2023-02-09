@file:Suppress("LeakingThis")

package io.novafoundation.nova.feature_governance_impl.presentation.tracks.select

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.toggle
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.common.chooseTrack.ChooseTrackInteractor
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.common.chooseTrack.model.ChooseTrackData
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.common.chooseTrack.model.TrackPreset
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.common.chooseTrack.model.hasUnavailableTracks
import io.novafoundation.nova.feature_governance_api.domain.track.Track
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.removeVotes.RemoveVotesPayload
import io.novafoundation.nova.feature_governance_impl.presentation.track.TrackFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.track.TrackModel
import io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.model.DelegationTrackModel
import io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.model.DelegationTracksPresetModel
import io.novafoundation.nova.runtime.state.chainAsset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.math.BigInteger

class UnavailableTracksModel(val alreadyVoted: List<TrackModel>, val alreadyDelegated: List<TrackModel>)

abstract class SelectDelegationTracksViewModel(
    private val interactor: ChooseTrackInteractor,
    private val trackFormatter: TrackFormatter,
    private val governanceSharedState: GovernanceSharedState,
    private val resourceManager: ResourceManager,
    private val router: GovernanceRouter,
    chooseTrackDataFlow: Flow<ChooseTrackData>
) : BaseViewModel() {

    protected abstract fun nextClicked(trackIds: List<BigInteger>)

    abstract val title: Flow<String>

    abstract val showDescription: Boolean

    private val chooseTrackDataFlowShared = chooseTrackDataFlow
        .inBackground()
        .shareWhileSubscribed()

    private val selectedTracksFlow = MutableStateFlow(setOf<TrackId>())

    private val trackPresetsFlow = chooseTrackDataFlowShared.map { it.presets }
        .shareWhileSubscribed()

    private val availableTrackFlow = chooseTrackDataFlowShared.map { it.trackPartition.available }
        .shareWhileSubscribed()

    private val _showRemoveVotesSuggestion = MutableLiveData<Event<Int>>()
    val showRemoveVotesSuggestion: LiveData<Event<Int>> = _showRemoveVotesSuggestion

    private val _showUnavailableTracksEvent = MutableLiveData<Event<UnavailableTracksModel>>()
    val showUnavailableTracksEvent: LiveData<Event<UnavailableTracksModel>> = _showUnavailableTracksEvent

    val trackPresetsModels = trackPresetsFlow
        .map(::mapTrackPresets)
        .shareWhileSubscribed()

    val availableTrackModels = combine(availableTrackFlow, selectedTracksFlow, ::mapTracksToModel)
        .withSafeLoading()
        .shareWhileSubscribed()

    val showUnavailableTracksButton = chooseTrackDataFlow
        .map { it.trackPartition.hasUnavailableTracks() }
        .shareWhileSubscribed()

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
        checkRemoveVotes()

        applyPreCheckedTracks()
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
            val tracksPartition = chooseTrackDataFlowShared.first().trackPartition
            val alreadyVotedModels = mapTracks(tracksPartition.alreadyVoted)
            val alreadyDelegatedModels = mapTracks(tracksPartition.alreadyDelegated)
            _showUnavailableTracksEvent.value = Event(UnavailableTracksModel(alreadyVotedModels, alreadyDelegatedModels))
        }
    }

    fun nextClicked() = launch {
        val selectedTrackIds = selectedTracksFlow.value
        nextClicked(selectedTrackIds.map(TrackId::value))
    }

    fun removeVotesSuggestionSkipped() = launch {
        interactor.disallowShowRemoveVotesSuggestion()
    }

    fun openRemoveVotesScreen() {
        launch {
            val chooseTrackData = chooseTrackDataFlowShared.first()
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

    private fun applyPreCheckedTracks() {
        launch {
            val preCheckedTrackIds = chooseTrackDataFlowShared.first().trackPartition.preCheckedTrackIds

            if (preCheckedTrackIds.isNotEmpty()) {
                selectedTracksFlow.value = preCheckedTrackIds
            }
        }
    }

    private fun checkRemoveVotes() {
        launch {
            val chooseTrackData = chooseTrackDataFlowShared.first()
            val alreadyVoted = chooseTrackData.trackPartition.alreadyVoted
            if (alreadyVoted.isNotEmpty() && interactor.isAllowedToShowRemoveVotesSuggestion()) {
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
