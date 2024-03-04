package io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.base

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
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.base.adapter.SelectTracksAdapter
import io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.base.adapter.SelectTracksPresetsAdapter
import kotlinx.android.synthetic.main.fragment_select_tracks.selectTracksList
import kotlinx.android.synthetic.main.fragment_select_tracks.selectTracksProgress
import kotlinx.android.synthetic.main.fragment_select_tracks.selectTracksToolbar

abstract class BaseSelectTracksFragment<V : BaseSelectTracksViewModel> :
    BaseFragment<V>(),
    SelectTracksAdapter.Handler,
    SelectTracksPresetsAdapter.Handler {

    abstract val headerAdapter: RecyclerView.Adapter<*>
    private val presetsAdapter = NestedAdapter(
        nestedAdapter = SelectTracksPresetsAdapter(this),
        orientation = RecyclerView.HORIZONTAL,
        paddingInDp = Rect(12, 8, 12, 12),
        disableItemAnimations = true
    )
    private val placeholderAdapter = CustomPlaceholderAdapter(R.layout.item_tracks_placeholder)
    private val tracksAdapter = SelectTracksAdapter(this)
    val adapter by lazy(LazyThreadSafetyMode.NONE) { ConcatAdapter(headerAdapter, presetsAdapter, placeholderAdapter, tracksAdapter) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_select_tracks, container, false)
    }

    override fun initViews() {
        selectTracksList.itemAnimator = null
        selectTracksList.adapter = adapter

        selectTracksToolbar.applyStatusBarInsets()
        selectTracksToolbar.setHomeButtonListener { viewModel.backClicked() }
    }

    override fun subscribe(viewModel: V) {
        viewModel.trackPresetsModels.observeWhenVisible {
            presetsAdapter.show(it.isNotEmpty())
            presetsAdapter.submitList(it)
        }

        viewModel.availableTrackModels.observeWhenVisible {
            selectTracksProgress.isVisible = it is ExtendedLoadingState.Loading
            when (it) {
                is ExtendedLoadingState.Error -> {}
                is ExtendedLoadingState.Loading -> placeholderAdapter.show(false)
                is ExtendedLoadingState.Loaded -> {
                    placeholderAdapter.show(it.data.isEmpty())
                    tracksAdapter.submitList(it.data)
                }
            }
        }
    }

    override fun trackClicked(position: Int) {
        viewModel.trackClicked(position)
    }

    override fun presetClicked(position: Int) {
        viewModel.presetClicked(position)
    }
}
