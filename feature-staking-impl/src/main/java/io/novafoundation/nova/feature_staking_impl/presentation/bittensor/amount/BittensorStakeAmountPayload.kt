package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.amount

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/** Root details always bind the wallet; an existing position also fixes its validator. */
@Parcelize
data class BittensorStakeAmountPayload(val metaId: Long, val validatorAddress: String?) : Parcelable
