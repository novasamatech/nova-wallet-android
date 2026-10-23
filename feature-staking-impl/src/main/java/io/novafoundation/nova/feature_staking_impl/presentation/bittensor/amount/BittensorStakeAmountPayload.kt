package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.amount

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/** Root details always bind the wallet; an existing position also fixes its validator. */
@Parcelize
data class BittensorStakeAmountPayload(val metaId: Long, val validatorAddress: String?) : Parcelable

/** How the root setup was reached (iOS `SubtensorRootEntryPresentation`): a quick pick shows the pick card. */
@Parcelize
enum class BittensorRootStakePresentation : Parcelable {
    STANDARD, QUICK_PICK
}

/** The three root setup layouts: quick pick (A-05 root), picker (A-11) and add stake (B-07). */
enum class BittensorRootStakeEntry {
    QUICK_PICK, PICKER, ADD_STAKE
}

internal fun bittensorRootStakeEntry(payload: BittensorStakeAmountPayload?, presentation: BittensorRootStakePresentation) = when {
    payload?.validatorAddress != null -> BittensorRootStakeEntry.ADD_STAKE
    presentation == BittensorRootStakePresentation.QUICK_PICK -> BittensorRootStakeEntry.QUICK_PICK
    else -> BittensorRootStakeEntry.PICKER
}
