package jp.co.soramitsu.feature_staking_impl.presentation.validators.details.model

import android.graphics.drawable.Drawable

class ValidatorDetailsModel(
    val stake: ValidatorStakeModel,
    val address: String,
    val addressImage: Drawable,
    val identity: IdentityModel?,
)
