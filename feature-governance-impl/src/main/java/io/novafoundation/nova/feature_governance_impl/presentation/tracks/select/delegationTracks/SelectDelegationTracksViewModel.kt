@file:Suppress("LeakingThis")

package io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.delegationTracks

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.common.chooseTrack.ChooseTrackInteractor
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.common.chooseTrack.model.ChooseTrackData
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.common.chooseTrack.model.hasUnavailableTracks
import io.novafoundation.nova.feature_governance_api.domain.track.Track
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_api.presentation.delegation.delegation.removeVotes.RemoveVotesPayload
import io.novafoundation.nova.feature_governance_impl.presentation.track.TrackFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.track.TrackModel
import io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.base.BaseSelectTracksViewModel
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.chainAsset
import kotlinx.coroutines.flow.Flow
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
) : BaseSelectTracksViewModel(
    trackFormatter = trackFormatter,
    resourceManager = resourceManager,
    router = router,
    chooseTrackDataFlow = chooseTrackDataFlow
) {

    protected abstract fun nextClicked(trackIds: List<BigInteger>)

    abstract val title: Flow<String>

    abstract val showDescription: Boolean

    private val _showRemoveVotesSuggestion = MutableLiveData<Event<Int>>()
    val showRemoveVotesSuggestion: LiveData<Event<Int>> = _showRemoveVotesSuggestion

    private val _showUnavailableTracksEvent = MutableLiveData<Event<UnavailableTracksModel>>()
    val showUnavailableTracksEvent: LiveData<Event<UnavailableTracksModel>> = _showUnavailableTracksEvent

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

    override suspend fun getChainAsset(): Chain.Asset {
        return governanceSharedState.chainAsset()
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

    private suspend fun mapTracks(tracks: List<Track>): List<TrackModel> {
        val asset = governanceSharedState.chainAsset()
        return tracks.map { trackFormatter.formatTrack(it, asset) }
    }
}
