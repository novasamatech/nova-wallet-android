package io.novafoundation.nova.feature_staking_impl.presentation.validators.current.model

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel

data class NominatedValidatorStatusModel(
    val titleConfig: TitleConfig?,
    val description: String,
) {
    data class TitleConfig(
        val text: String,
        @DrawableRes val iconRes: Int,
        @ColorRes val iconTintRes: Int,
        @ColorRes val textColorRes: Int
    )
}

class NominatedValidatorModel(
    val addressModel: AddressModel,
    val nominated: AmountModel?,
    val apy: String?,
    val isOversubscribed: Boolean,
    val isSlashed: Boolean
)
