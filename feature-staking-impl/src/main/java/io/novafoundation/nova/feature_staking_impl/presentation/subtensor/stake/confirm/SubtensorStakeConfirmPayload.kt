package io.novafoundation.nova.feature_staking_impl.presentation.subtensor.stake.confirm

import android.os.Parcelable
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeParcelModel
import kotlinx.parcelize.Parcelize
import java.math.BigInteger

/**
 * Payload threaded from the Subtensor stake setup screen into Confirm.
 * Mirrors iOS `SubtensorStakeConfirmViewFactory.createView` argument set
 * (chainAsset, validator, amount). Fee is snapshotted on Setup so what
 * the user agreed to is what they sign — Nova's other Confirm flows use
 * the same idiom (see ConfirmBondMorePayload).
 */
@Parcelize
data class SubtensorStakeConfirmPayload(
    val netuid: Int,
    val hotkeyAddress: String,
    /** Validator identity name from delegates registry; null when unknown. */
    val validatorIdentity: String?,
    val amountInPlanks: BigInteger,
    val fee: FeeParcelModel,
    val subnetName: String?,
) : Parcelable
