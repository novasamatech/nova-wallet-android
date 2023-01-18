package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes

class DelegateTypeModel(
    val text: String,
    @DrawableRes val iconRes: Int,
    @ColorRes val textColorRes: Int,
    @ColorRes val iconColorRes: Int,
    @ColorRes val backgroundColorRes: Int,
    val delegateIconShape: IconShape
) {

    enum class IconShape {
        ROUND, SQUARE
    }
}
