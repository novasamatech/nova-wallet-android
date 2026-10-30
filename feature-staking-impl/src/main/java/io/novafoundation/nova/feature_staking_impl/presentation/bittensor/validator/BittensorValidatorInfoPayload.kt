package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.validator

import android.os.Parcelable
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetValidator
import java.math.BigDecimal
import kotlinx.parcelize.Parcelize

/**
 * The validator to show. Without a [detail], as from a position, a review or a claim, the screen reads it from the listing and
 * the chain. It loads the reward rate and the price itself in any case.
 */
@Parcelize
class BittensorValidatorInfoPayload(
    val subnetId: Int,
    val address: String,
    val detail: Detail? = null
) : Parcelable {

    /** The validator as the list row knows it. [reportedStake] is the directory stake in the subnet token (TAO on root). */
    @Parcelize
    class Detail(
        val name: String?,
        val reportedStake: BigDecimal?,
        val commissionRate: BigDecimal,
        val inactive: Boolean,
        val isViaChildKeys: Boolean = false
    ) : Parcelable
}

fun BittensorSubnetValidator.toValidatorInfoDetail() = BittensorValidatorInfoPayload.Detail(
    name = validator.name.trim().takeIf { it.isNotEmpty() && it != address },
    reportedStake = validator.reportedStake,
    commissionRate = commissionRate,
    inactive = inactive,
    isViaChildKeys = isViaChildKeys
)
