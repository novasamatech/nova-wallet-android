package io.novafoundation.nova.feature_assets.presentation.swap.asset

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.core.view.doOnLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import coil.ImageLoader
import com.google.android.material.bottomsheet.BottomSheetBehavior
import io.novafoundation.nova.common.base.BaseBottomSheetFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.list.CustomPlaceholderAdapter
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.keyboard.hideSoftKeyboard
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawableWithRipple
import io.novafoundation.nova.common.view.shape.getTopRoundedCornerDrawable
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.databinding.FragmentSwapTokenSelectionBinding
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_swap_api.presentation.navigation.SwapTokenSelectionCommunicator
import javax.inject.Inject
import kotlin.math.min

class AssetSwapFlowFragment : BaseBottomSheetFragment<AssetSwapFlowViewModel, FragmentSwapTokenSelectionBinding>() {

    companion object {
        private const val KEY_PAYLOAD = "AssetSwapFlowFragment.payload"
        fun getBundle(request: SwapTokenSelectionCommunicator.Request) = Bundle().apply { putParcelable(KEY_PAYLOAD, request) }
    }

    @Inject lateinit var imageLoader: ImageLoader

    private val tokensAdapter by lazy { SwapTokenSelectionAdapter(imageLoader, ::tokenClicked) }
    private val chipsAdapter by lazy { SwapNetworkChipAdapter(imageLoader, ::chipClicked) }
    private val loadingAdapter = CustomPlaceholderAdapter(R.layout.item_swap_tokens_loading)
    private var model: SwapTokenSelectionModel? = null
    private var networksSheet: SwapNetworksBottomSheet? = null
    private var bottomInset = 0
    private var availableHeight = 0

    override fun createBinding() = FragmentSwapTokenSelectionBinding.inflate(layoutInflater)

    override fun inject() {
        FeatureUtils.getFeature<AssetsFeatureComponent>(requireContext(), AssetsFeatureApi::class.java)
            .swapFlowComponent().create(this, argument(KEY_PAYLOAD)).inject(this)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.doOnLayout { sheet ->
            sheet.backgroundTintList = null
            sheet.setBackgroundColor(Color.TRANSPARENT)
        }
        getBehaviour().apply {
            skipCollapsed = false
            state = BottomSheetBehavior.STATE_COLLAPSED
        }
        resizeSheet()
    }

    override fun initViews() {
        binder.root.background = requireContext().getTopRoundedCornerDrawable(R.color.bottom_sheet_background, cornerSizeInDp = 16)
        binder.swapTokenList.adapter = ConcatAdapter(loadingAdapter, tokensAdapter)
        binder.swapTokenList.itemAnimator = null
        binder.swapTokenChips.adapter = chipsAdapter
        binder.swapTokenChips.itemAnimator = null
        binder.swapTokenClose.background = requireContext().getRoundedCornerDrawableWithRipple(R.color.chips_background, cornerSizeInDp = 22)
        binder.swapTokenClose.setOnClickListener { viewModel.close() }
        binder.swapTokenPlaceholder.setButtonClickListener { viewModel.retry() }
        binder.swapTokenPlaceholder.setImageTint(null)
        ViewCompat.setOnApplyWindowInsetsListener(binder.root) { _, insets ->
            bottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            availableHeight = resources.displayMetrics.heightPixels - status - maxOf(bottomInset, ime)
            binder.root.setPadding(0, 0, 0, 16.dp + if (ime > bottomInset) 0 else bottomInset)
            resizeSheet()
            insets
        }
    }

    override fun subscribe(viewModel: AssetSwapFlowViewModel) {
        binder.swapTokenSearch.content.bindTo(viewModel.query, viewLifecycleOwner.lifecycleScope)
        viewModel.screen.observeWhenVisible(::render)
    }

    private fun render(newModel: SwapTokenSelectionModel) {
        model = newModel
        binder.swapTokenTitle.text = newModel.title
        chipsAdapter.submitList(newModel.chips)
        networksSheet?.setNetworks(newModel.networks)
        loadingAdapter.show(newModel.loading)
        tokensAdapter.submitList(newModel.items)
        val empty = !newModel.loading && newModel.items.isEmpty()
        binder.swapTokenPlaceholder.isVisible = empty
        binder.swapTokenPlaceholder.setText(if (newModel.failed) R.string.swap_select_loading_error else R.string.assets_search_placeholder)
        binder.swapTokenPlaceholder.setButtonText(if (newModel.failed) getString(R.string.common_retry) else null)
        binder.swapTokenSelecting.isVisible = newModel.selecting
        resizeSheet()
    }

    private fun resizeSheet() {
        val height = if (availableHeight > 0) availableHeight else resources.displayMetrics.heightPixels
        val collapsedHeight = 626.dp + bottomInset
        if (binder.root.layoutParams.height != height) binder.root.updateLayoutParams { this.height = height }
        getBehaviour().peekHeight = min(collapsedHeight, height)
    }

    private fun finishSearch() {
        binder.swapTokenSearch.content.hideSoftKeyboard()
        binder.root.requestFocus()
    }

    private fun tokenClicked(row: SwapTokenRow) {
        finishSearch()
        viewModel.rowClicked(row)
    }

    private fun chipClicked(chip: SwapNetworkChip) {
        if (chip is SwapNetworkChip.More) {
            showNetworkFilter()
        } else {
            viewModel.chipClicked(chip)
            binder.swapTokenList.scrollToPosition(0)
        }
    }

    private fun showNetworkFilter() {
        val model = model ?: return
        finishSearch()
        networksSheet?.dismiss()
        networksSheet = SwapNetworksBottomSheet(requireContext(), imageLoader, model.networks, viewModel::saveNetworks).also { sheet ->
            sheet.setOnDismissListener { networksSheet = null }
            sheet.show()
        }
    }

    override fun onDestroyView() {
        networksSheet?.dismiss()
        networksSheet = null
        binder.swapTokenChips.adapter = null
        binder.swapTokenList.adapter = null
        super.onDestroyView()
    }
}
