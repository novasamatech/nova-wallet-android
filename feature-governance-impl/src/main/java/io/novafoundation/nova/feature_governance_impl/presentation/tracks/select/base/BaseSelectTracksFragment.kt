package io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.base

import android.graphics.Rect
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.list.NestedAdapter
import io.novafoundation.nova.common.list.CustomPlaceholderAdapter
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.databinding.FragmentSelectTracksBinding
import io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.base.adapter.SelectTracksAdapter
import io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.base.adapter.SelectTracksPresetsAdapter

abstract class BaseSelectTracksFragment<V : BaseSelectTracksViewModel> :
    BaseFragment<V, FragmentSelectTracksBinding>(),
    SelectTracksAdapter.Handler,
    SelectTracksPresetsAdapter.Handler {

    override val binder by viewBinding(FragmentSelectTracksBinding::bind)

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

    override fun initViews() {
        binder.selectTracksList.itemAnimator = null
        binder.selectTracksList.adapter = adapter

        binder.selectTracksToolbar.applyStatusBarInsets()
        binder.selectTracksToolbar.setHomeButtonListener { viewModel.backClicked() }
    }

    override fun subscribe(viewModel: V) {
        viewModel.trackPresetsModels.observeWhenVisible {
            presetsAdapter.show(it.isNotEmpty())
            presetsAdapter.submitList(it)
        }

        viewModel.availableTrackModels.observeWhenVisible {
            binder.selectTracksProgress.isVisible = it is ExtendedLoadingState.Loading
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
