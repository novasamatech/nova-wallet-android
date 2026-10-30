package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import androidx.annotation.ColorRes
import androidx.core.view.isVisible
import coil.ImageLoader
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.images.setIconOrMakeGone
import io.novafoundation.nova.common.utils.setDrawableEnd
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.utils.formatting.formatAsPercentage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.ViewBittensorValidatorRowBinding
import java.math.BigDecimal

/** A validator with its estimated APY under the name, as iOS shows it on setup and review ("Est. 33.99% APY"). */
class BittensorValidatorRowUi(
    val name: CharSequence,
    val icon: Icon?,
    val rate: CharSequence?,
    @ColorRes val rateColorRes: Int = R.color.text_positive
)

/** "Est. 6.18% APY", coloured by sign; null while the rate is unknown so the row shows the name alone. */
internal fun ResourceManager.bittensorLeadingApy(annualYield: BigDecimal?): String? {
    return annualYield?.let { getString(R.string.bittensor_apy_leading, it.formatAsPercentage()) }
}

fun ViewBittensorValidatorRowBinding.setSelectable(selectable: Boolean) {
    root.isClickable = selectable
    root.isFocusable = selectable
    validatorRowChevron.isVisible = selectable
}

fun ViewBittensorValidatorRowBinding.showTitleInfo() {
    validatorRowTitle.setDrawableEnd(R.drawable.ic_info, widthInDp = 16, paddingInDp = 4)
}

fun ViewBittensorValidatorRowBinding.showValidator(state: ExtendedLoadingState<BittensorValidatorRowUi?>, imageLoader: ImageLoader) {
    val loading = state is ExtendedLoadingState.Loading
    validatorRowProgress.isVisible = loading
    validatorRowContent.isVisible = !loading
    if (loading) {
        validatorRowRate.isVisible = false
        return
    }
    val validator = (state as? ExtendedLoadingState.Loaded)?.data
    validatorRowName.text = validator?.name ?: "—"
    validatorRowIcon.setIconOrMakeGone(validator?.icon, imageLoader)
    validatorRowRate.setTextOrHide(validator?.rate)
    validator?.let { validatorRowRate.setTextColorRes(it.rateColorRes) }
}
