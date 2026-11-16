package io.novafoundation.nova.feature_assets.presentation.swap.asset

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import coil.ImageLoader
import com.google.android.material.bottomsheet.BottomSheetBehavior
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.view.bottomSheet.BaseBottomSheet
import io.novafoundation.nova.common.view.shape.getTopRoundedCornerDrawable
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.databinding.BottomSheetSwapNetworksBinding
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

internal class SwapNetworksBottomSheet(
    context: Context,
    imageLoader: ImageLoader,
    networks: List<SwapNetworkFilter>,
    private val onDone: (Set<ChainId>) -> Unit
) : BaseBottomSheet<BottomSheetSwapNetworksBinding>(context) {

    override val binder = BottomSheetSwapNetworksBinding.inflate(LayoutInflater.from(context))
    private var networks = networks
    private var selected = networks.filter { it.selected }.map { it.chainId }.toSet()
    private val adapter = SwapNetworkFilterAdapter(imageLoader) { network ->
        selected = if (network.chainId in selected) selected - network.chainId else selected + network.chainId
        render()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binder.root.updateLayoutParams { height = minOf(512.dp(context), context.resources.displayMetrics.heightPixels) }
        findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.updateLayoutParams { height = ViewGroup.LayoutParams.WRAP_CONTENT }
        binder.root.background = context.getTopRoundedCornerDrawable(R.color.bottom_sheet_background, cornerSizeInDp = 16)
        binder.swapNetworksList.adapter = adapter
        binder.swapNetworksList.itemAnimator = null
        binder.swapNetworksDone.setOnClickListener {
            onDone(selected.intersect(networks.map { it.chainId }.toSet()))
            dismiss()
        }
        ViewCompat.setOnApplyWindowInsetsListener(binder.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val availableHeight = context.resources.displayMetrics.heightPixels - systemBars.top - systemBars.bottom
            binder.root.updateLayoutParams { height = minOf(512.dp(context), availableHeight) }
            binder.root.setPadding(0, 0, 0, 16.dp(context))
            insets
        }
        render()
    }

    override fun onStart() {
        super.onStart()
        findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.doOnLayout { sheet ->
            sheet.backgroundTintList = null
            sheet.setBackgroundColor(Color.TRANSPARENT)
        }
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true
        ViewCompat.requestApplyInsets(binder.root)
    }

    fun setNetworks(networks: List<SwapNetworkFilter>) {
        this.networks = networks
        render()
    }

    private fun render() {
        adapter.submitList(networks.map { it.copy(selected = it.chainId in selected) })
    }
}
