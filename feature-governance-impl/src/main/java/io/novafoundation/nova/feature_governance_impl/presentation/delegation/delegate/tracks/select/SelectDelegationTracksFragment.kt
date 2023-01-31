package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.tracks.select

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.list.NestedAdapter
import io.novafoundation.nova.common.list.PlaceholderAdapter
import io.novafoundation.nova.common.presentation.ExtendedLoadingState
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.tracks.select.adapter.SelectDelegationTracksAdapter
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.tracks.select.adapter.SelectDelegationTracksHeaderAdapter
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.tracks.select.adapter.SelectDelegationTracksPresetsAdapter
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.removeVotes.RemoveVotesSuggestionBottomSheet
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.android.synthetic.main.fragment_select_delegation_tracks.selectDelegationTracksApply
import kotlinx.android.synthetic.main.fragment_select_delegation_tracks.selectDelegationTracksList
import kotlinx.android.synthetic.main.fragment_select_delegation_tracks.selectDelegationTracksProgress
import kotlinx.android.synthetic.main.fragment_select_delegation_tracks.selectDelegationTracksToolbar

class SelectDelegationTracksFragment :
    BaseFragment<SelectDelegationTracksViewModel>(),
    SelectDelegationTracksAdapter.Handler,
    SelectDelegationTracksHeaderAdapter.Handler,
    SelectDelegationTracksPresetsAdapter.Handler {

    companion object {
        private const val EXTRA_PAYLOAD = "EXTRA_PAYLOAD"

        fun newBundle(accountId: AccountId): Bundle {
            return Bundle().apply {
                putParcelable(EXTRA_PAYLOAD, SelectDelegationTracksPayload(accountId))
            }
        }
    }

    private val headerAdapter by lazy(LazyThreadSafetyMode.NONE) { SelectDelegationTracksHeaderAdapter(this) }
    private val presetsAdapter by lazy(LazyThreadSafetyMode.NONE) {
        NestedAdapter(SelectDelegationTracksPresetsAdapter(this), RecyclerView.HORIZONTAL, Rect(12.dp, 8.dp, 12.dp, 12.dp))
    }
    private val placeholderAdapter by lazy(LazyThreadSafetyMode.NONE) { PlaceholderAdapter(R.layout.item_tracks_placeholder) }
    private val tracksAdapter by lazy(LazyThreadSafetyMode.NONE) { SelectDelegationTracksAdapter(this) }
    private val concatAdapter by lazy(LazyThreadSafetyMode.NONE) { ConcatAdapter(headerAdapter, presetsAdapter, placeholderAdapter, tracksAdapter) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_select_delegation_tracks, container, false)
    }

    override fun initViews() {
        selectDelegationTracksList.itemAnimator = null
        selectDelegationTracksList.setHasFixedSize(true)
        selectDelegationTracksList.adapter = concatAdapter

        selectDelegationTracksToolbar.applyStatusBarInsets()
        selectDelegationTracksToolbar.setHomeButtonListener { viewModel.backClicked() }

        selectDelegationTracksApply.setOnClickListener { viewModel.openSetupConviction() }
    }

    override fun inject() {
        FeatureUtils.getFeature<GovernanceFeatureComponent>(
            requireContext(),
            GovernanceFeatureApi::class.java
        ).selectDelegationTracks()
            .create(this, argument(EXTRA_PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: SelectDelegationTracksViewModel) {
        viewModel.showUnavailableTracksButton.observe {
            headerAdapter.showUnavailableTracks(it)
        }

        viewModel.trackPresetsModels.observe {
            presetsAdapter.show(it.isNotEmpty())
            presetsAdapter.submitList(it)
        }

        viewModel.availableTrackModels.observe {
            selectDelegationTracksProgress.isVisible = it is ExtendedLoadingState.Loading
            when (it) {
                is ExtendedLoadingState.Error -> {}
                is ExtendedLoadingState.Loading -> placeholderAdapter.showPlaceholder(false)
                is ExtendedLoadingState.Loaded -> {
                    placeholderAdapter.showPlaceholder(it.data.isEmpty())
                    tracksAdapter.submitList(it.data)
                }
            }
        }

        viewModel.isButtonEnabled.observe {
            selectDelegationTracksApply.isEnabled = it
            selectDelegationTracksApply.text = if (it) {
                getString(R.string.common_continue)
            } else {
                getString(R.string.delegation_tracks_disabled_apply_button_text)
            }
        }

        viewModel.showRemoveVotesSuggestion.observeEvent {
            val bottomSheet = RemoveVotesSuggestionBottomSheet(
                requireContext(),
                onApply = viewModel::openRemoveVotesScreen
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
