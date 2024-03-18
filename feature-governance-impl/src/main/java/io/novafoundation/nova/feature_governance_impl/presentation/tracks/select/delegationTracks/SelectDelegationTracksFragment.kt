package io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.delegationTracks

import androidx.core.view.isVisible
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.removeVotes.RemoveVotesSuggestionBottomSheet
import io.novafoundation.nova.feature_governance_impl.presentation.track.unavailable.UnavailableTracksBottomSheet
import io.novafoundation.nova.feature_governance_impl.presentation.track.unavailable.UnavailableTracksPayload
import io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.base.BaseSelectTracksFragment
import io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.base.adapter.SelectTracksAdapter
import io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.delegationTracks.adapter.SelectTracksHeaderAdapter
import io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.base.adapter.SelectTracksPresetsAdapter
import kotlinx.android.synthetic.main.fragment_select_tracks.selectTracksApply

abstract class SelectDelegationTracksFragment<V : SelectDelegationTracksViewModel> :
    BaseSelectTracksFragment<V>(),
    SelectTracksAdapter.Handler,
    SelectTracksHeaderAdapter.Handler,
    SelectTracksPresetsAdapter.Handler {

    override val headerAdapter = SelectTracksHeaderAdapter(this)

    override fun initViews() {
        super.initViews()
        selectTracksApply.isVisible = true
        selectTracksApply.setOnClickListener { viewModel.nextClicked() }
    }

    override fun subscribe(viewModel: V) {
        super.subscribe(viewModel)
        headerAdapter.setShowDescription(viewModel.showDescription)

        viewModel.title.observeWhenVisible(headerAdapter::setTitle)

        viewModel.showUnavailableTracksButton.observeWhenVisible {
            headerAdapter.showUnavailableTracks(it)
        }

        viewModel.buttonState.observeWhenVisible(selectTracksApply::setState)

        viewModel.showRemoveVotesSuggestion.observeEvent {
            val bottomSheet = RemoveVotesSuggestionBottomSheet(
                context = requireContext(),
                votesCount = it,
                onApply = viewModel::openRemoveVotesScreen,
                onSkip = viewModel::removeVotesSuggestionSkipped
            )
            bottomSheet.show()
        }

        viewModel.showUnavailableTracksEvent.observeEvent {
            val bottomSheet = UnavailableTracksBottomSheet(
                requireContext(),
                UnavailableTracksPayload(
                    alreadyVoted = it.alreadyVoted,
                    alreadyDelegated = it.alreadyDelegated
                ),
                viewModel::openRemoveVotesScreen
            )
            bottomSheet.show()
        }
    }

    override fun unavailableTracksClicked() {
        viewModel.unavailableTracksClicked()
    }
}
