package io.novafoundation.nova.feature_staking_impl.presentation.subtensor.unstake.confirm

import android.os.Parcelable
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeParcelModel
import kotlinx.parcelize.Parcelize
import java.math.BigInteger

/**
 * Payload threaded from the Subtensor unstake setup screen into Confirm.
 * Mirrors iOS `SubtensorUnstakeConfirmViewFactory` argument set
 * (chainAsset, position, amount). [feeInPlanks] is the snapshot taken on
 * Setup so what the user agreed to is what they sign.
 */
@Parcelize
data class SubtensorUnstakeConfirmPayload(
    val netuid: Int,
    val hotkeyAddress: String,
    /** Validator identity name from delegates registry; null when unknown. */
    val validatorIdentity: String?,
    /** Amount the user is unstaking — TAO planks for root, alpha planks for subnet. */
    val amountInPlanks: BigInteger,
    /** Full position planks; used by the dust-remainder warning hint. */
    val positionPlanks: BigInteger,
    val fee: FeeParcelModel,
) : Parcelable
