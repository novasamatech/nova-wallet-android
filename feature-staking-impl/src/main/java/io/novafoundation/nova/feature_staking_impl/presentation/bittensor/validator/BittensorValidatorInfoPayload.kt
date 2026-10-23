package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.validator

import android.os.Parcelable
import java.math.BigDecimal
import kotlinx.parcelize.Parcelize

/**
 * The validator as the list row knows it. [reportedStake] is the directory stake in the subnet token (TAO on root); the screen
 * loads the reward rate and the price itself, so a tap before the list has them still fills in.
 */
@Parcelize
class BittensorValidatorInfoPayload(
    val subnetId: Int,
    val address: String,
    val name: String?,
    val reportedStake: BigDecimal?,
    val commissionRate: BigDecimal,
    val inactive: Boolean
) : Parcelable
