package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import android.graphics.drawable.Drawable
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetValidator

data class BittensorValidatorUi(val eligibility: BittensorSubnetValidator, val icon: Drawable)
