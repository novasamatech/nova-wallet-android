package io.novafoundation.nova.feature_staking_impl.presentation.validators.details.model

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel

class ValidatorStakeModel(
    val status: Status,
    val activeStakeModel: ActiveStakeModel?,
) {

    class Status(
        val text: String,
        @DrawableRes val icon: Int,
        @ColorRes val iconTint: Int,
    )

    class ActiveStakeModel(
        val totalStake: AmountModel,
        val minimumStake: AmountModel?,
        val nominatorsCount: String,
        val maxNominations: String?,
        val apy: String
    )
}
