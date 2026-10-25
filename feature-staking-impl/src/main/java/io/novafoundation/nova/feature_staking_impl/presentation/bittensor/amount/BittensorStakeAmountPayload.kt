package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.amount

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Root details always bind the wallet; an existing position also fixes its validator.
 * Without [metaId] the screen follows the selected wallet; [quickPick] marks the Where to stake root card.
 */
@Parcelize
data class BittensorStakeAmountPayload(
    val metaId: Long?,
    val validatorAddress: String?,
    val quickPick: Boolean = false
) : Parcelable {

    init {
        require(metaId != null || validatorAddress == null) { "An existing position requires its wallet" }
    }
}
