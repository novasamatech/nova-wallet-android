package io.novafoundation.nova.feature_wallet_api.presentation.model

import androidx.annotation.DimenRes

sealed interface FractionStylingSize {
    data object Default : FractionStylingSize
    data class AbsoluteSize(@DimenRes val sizeRes: Int) : FractionStylingSize
}
