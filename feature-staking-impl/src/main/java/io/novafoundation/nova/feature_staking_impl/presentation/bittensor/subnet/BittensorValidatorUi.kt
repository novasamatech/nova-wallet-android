package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import android.graphics.drawable.Drawable
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetValidator
import io.novafoundation.nova.feature_staking_impl.R
import java.math.BigDecimal

data class BittensorValidatorUi(
    val eligibility: BittensorSubnetValidator,
    val icon: Drawable,
    val apy: String? = null,
    val apyColorRes: Int = R.color.text_secondary,
    val subtitle: String? = null,
    val stakeInNative: BigDecimal? = null,
    val annualYield: BigDecimal? = null
)
