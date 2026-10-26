package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import android.os.Parcelable
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.parcelize.Parcelize

@Parcelize
class BittensorSubnetReviewPayload(
    val subnetId: Int,
    val amountInPlanks: BigInteger,
    val metaId: Long,
    val validatorAddress: String,
    val slippage: BigDecimal,
    val existingPosition: Boolean = false,
    val inactiveValidatorAccepted: Boolean = false
) : Parcelable
