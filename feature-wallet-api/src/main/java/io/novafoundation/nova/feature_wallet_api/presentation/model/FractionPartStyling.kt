package io.novafoundation.nova.feature_wallet_api.presentation.model

import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import io.novafoundation.nova.feature_wallet_api.R

sealed interface FractionPartStyling {
    data object NoStyle : FractionPartStyling
    data class Styled(
        @DimenRes val sizeRes: Int,
        @ColorRes val colorRes: Int = R.color.text_secondary
    ) : FractionPartStyling
}
