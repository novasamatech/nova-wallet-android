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

/** The three root setup layouts: quick pick (A-05 root), picker (A-11) and add stake (B-07). */
enum class BittensorRootStakeEntry {
    QUICK_PICK, PICKER, ADD_STAKE
}

internal fun bittensorRootStakeEntry(payload: BittensorStakeAmountPayload?) = when {
    payload?.validatorAddress != null -> BittensorRootStakeEntry.ADD_STAKE
    payload?.quickPick == true -> BittensorRootStakeEntry.QUICK_PICK
    else -> BittensorRootStakeEntry.PICKER
}
