package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.common.view.TableCellView
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentBittensorSubnetDetailsBinding
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.showBittensorLoadingAmount
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.showBittensorLoadingValue
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel

fun FragmentBittensorSubnetDetailsBinding.bindSubnetParameters(model: BittensorSubnetParametersUi) {
    pool.showBittensorLoadingAmount(model.nativePool.map { AmountModel(it.amount, it.fiat) })
    nativePool.bindPoolSide(model.nativePool)
    alphaPool.bindPoolSide(model.alphaPool)
    emission.showBittensorLoadingValue(model.emission)

    with(poolSplit) {
        bars.isVisible = false
        when (val state = model.split) {
            ExtendedLoadingState.Loading -> {
                nativeShareValue.showShare(nativeShareProgress, null)
                alphaShareValue.showShare(alphaShareProgress, null)
            }
            is ExtendedLoadingState.Error -> {
                nativeShareValue.showShare(nativeShareProgress, "—")
                alphaShareValue.showShare(alphaShareProgress, "—")
            }
            is ExtendedLoadingState.Loaded -> {
                val split = state.data
                nativeShareTitle.text = split.nativeTitle
                alphaShareTitle.text = split.alphaTitle
                nativeShareValue.showShare(nativeShareProgress, split.nativeShare)
                alphaShareValue.showShare(alphaShareProgress, split.alphaShare)
                val fraction = split.nativeFraction?.takeIf { it.isFinite() && it in 0f..1f }
                if (fraction != null) {
                    bars.isVisible = true
                    nativeBar.isVisible = fraction > 0f
                    alphaBar.isVisible = fraction < 1f
                    gap.isVisible = fraction > 0f && fraction < 1f
                    nativeBar.updateLayoutParams<LinearLayout.LayoutParams> { weight = fraction }
                    alphaBar.updateLayoutParams<LinearLayout.LayoutParams> { weight = 1f - fraction }
                    nativeBar.background = root.context.getRoundedCornerDrawable(R.color.text_positive, cornerSizeInDp = 3)
                    alphaBar.background = root.context.getRoundedCornerDrawable(R.color.button_background_primary, cornerSizeInDp = 3)
                }
            }
        }
    }
}

/** `null` shows the progress in place of the share. */
private fun TextView.showShare(progress: View, share: CharSequence?) {
    isVisible = share != null
    progress.isVisible = share == null
    text = share
}

private fun TableCellView.bindPoolSide(state: ExtendedLoadingState<BittensorSubnetPoolSideUi>) {
    when (state) {
        ExtendedLoadingState.Loading -> showProgress()
        is ExtendedLoadingState.Error -> showValue("—")
        is ExtendedLoadingState.Loaded -> {
            setTitle(state.data.title)
            showValue(state.data.amount, state.data.fiat)
        }
    }
}
