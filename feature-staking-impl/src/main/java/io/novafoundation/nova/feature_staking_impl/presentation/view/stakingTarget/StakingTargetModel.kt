package io.novafoundation.nova.feature_staking_impl.presentation.view.stakingTarget

import androidx.annotation.ColorRes

data class StakingTargetModel(
    val title: String,
    val subtitle: String?,
    @ColorRes val subtitleColorRes: Int,
    val icon: Icon?
) {

    sealed interface Icon {

        data class Quantity(val quantity: String) : Icon

        class Drawable(val drawable: android.graphics.drawable.Drawable) : Icon
    }
}
