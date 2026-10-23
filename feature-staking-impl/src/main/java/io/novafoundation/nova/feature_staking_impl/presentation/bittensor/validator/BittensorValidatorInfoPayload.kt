package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.validator

import android.os.Parcelable
import java.math.BigDecimal
import kotlinx.parcelize.Parcelize

/**
 * The validator as the list shows it: the info screen presents the same snapshot instead of re-deriving it.
 * [stakeInNative] is the reported stake converted to the native token, [annualYield] the same rate the row shows.
 */
@Parcelize
class BittensorValidatorInfoPayload(
    val subnetId: Int,
    val address: String,
    val name: String?,
    val stakeInNative: BigDecimal?,
    val commissionRate: BigDecimal,
    val inactive: Boolean,
    val annualYield: BigDecimal?
) : Parcelable
