package io.novafoundation.nova.feature_staking_impl.presentation.view.stakingTarget

import androidx.annotation.ColorRes

class StakingTargetModel(
    val title: String,
    val subtitle: String?,
    @ColorRes val subtitleColor: Int,
    val icon: Icon?
) {

    sealed interface Icon {

        class Quantity(val quantity: String) : Icon

        class Drawable(val drawable: android.graphics.drawable.Drawable) : Icon
    }
}
