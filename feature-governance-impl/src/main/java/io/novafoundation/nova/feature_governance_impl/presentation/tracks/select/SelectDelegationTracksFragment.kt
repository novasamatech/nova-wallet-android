package io.novafoundation.nova.feature_governance_impl.presentation.tracks.select

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.list.NestedAdapter
import io.novafoundation.nova.common.list.CustomPlaceholderAdapter
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.removeVotes.RemoveVotesSuggestionBottomSheet
import io.novafoundation.nova.feature_governance_impl.presentation.track.unavailable.UnavailableTracksBottomSheet
import io.novafoundation.nova.feature_governance_impl.presentation.track.unavailable.UnavailableTracksPayload
import io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.adapter.SelectDelegationTracksAdapter
import io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.adapter.SelectDelegationTracksHeaderAdapter
import io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.adapter.SelectDelegationTracksPresetsAdapter
import kotlinx.android.synthetic.main.fragment_select_delegation_tracks.selectDelegationTracksApply
import kotlinx.android.synthetic.main.fragment_select_delegation_tracks.selectDelegationTracksList
import kotlinx.android.synthetic.main.fragment_select_delegation_tracks.selectDelegationTracksProgress
import kotlinx.android.synthetic.main.fragment_select_delegation_tracks.selectDelegationTracksToolbar

abstract class SelectDelegationTracksFragment<V : SelectDelegationTracksViewModel> :
    BaseFragment<V>(),
    SelectDelegationTracksAdapter.Handler,
    SelectDelegationTracksHeaderAdapter.Handler,
    SelectDelegationTracksPresetsAdapter.Handler {

    private val headerAdapter = SelectDelegationTracksHeaderAdapter(this)

    private val presetsAdapter = NestedAdapter(
        nestedAdapter = SelectDelegationTracksPresetsAdapter(this),
        orientation = RecyclerView.HORIZONTAL,
        paddingInDp = Rect(12, 8, 12, 12),
        disableItemAnimations = true
    )
    private val placeholderAdapter = CustomPlaceholderAdapter(R.layout.item_tracks_placeholder)
    private val tracksAdapter = SelectDelegationTracksAdapter(this)
    private val concatAdapter = ConcatAdapter(headerAdapter, presetsAdapter, placeholderAdapter, tracksAdapter)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_select_delegation_tracks, container, false)
    }

    override fun initViews() {
        selectDelegationTracksList.itemAnimator = null
        selectDelegationTracksList.adapter = concatAdapter

        selectDelegationTracksToolbar.applyStatusBarInsets()
        selectDelegationTracksToolbar.setHomeButtonListener { viewModel.backClicked() }

        selectDelegationTracksApply.setOnClickListener { viewModel.nextClicked() }
    }

    override fun subscribe(viewModel: V) {
        headerAdapter.setShowDescription(viewModel.showDescription)

        viewModel.title.observeWhenVisible(headerAdapter::setTitle)

        viewModel.showUnavailableTracksButton.observeWhenVisible {
            headerAdapter.showUnavailableTracks(it)
        }

        viewModel.trackPresetsModels.observeWhenVisible {
            presetsAdapter.show(it.isNotEmpty())
            presetsAdapter.submitList(it)
        }

        viewModel.availableTrackModels.observeWhenVisible {
            selectDelegationTracksProgress.isVisible = it is ExtendedLoadingState.Loading
            when (it) {
                is ExtendedLoadingState.Error -> {}
                is ExtendedLoadingState.Loading -> placeholderAdapter.show(false)
                is ExtendedLoadingState.Loaded -> {
                    placeholderAdapter.show(it.data.isEmpty())
                    tracksAdapter.submitList(it.data)
                }
            }
        }

        viewModel.buttonState.observeWhenVisible(selectDelegationTracksApply::setState)

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

    override fun trackClicked(position: Int) {
        viewModel.trackClicked(position)
    }

    override fun unavailableTracksClicked() {
        viewModel.unavailableTracksClicked()
    }

    override fun presetClicked(position: Int) {
        viewModel.presetClicked(position)
    }
}
