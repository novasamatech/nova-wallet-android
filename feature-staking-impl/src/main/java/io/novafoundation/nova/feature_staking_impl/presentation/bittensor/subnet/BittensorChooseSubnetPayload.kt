package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BittensorChooseSubnetPayload(
    val replaceAmount: Boolean = false,
    val browseState: BittensorSubnetBrowseState = BittensorSubnetBrowseState(),
    val selectionState: BittensorSelectionState = BittensorSelectionState(),
    /** The setup screen a [replaceAmount] pick replaces (iOS swaps the setup mode in place). */
    val replaceOrigin: BittensorReplaceOrigin = BittensorReplaceOrigin.SUBNET_AMOUNT,
    /** The replaced setup was a quick pick, so a root pick keeps the quick-pick layout (iOS `quickPickOrder != nil`). */
    val quickPick: Boolean = false
) : Parcelable

@Parcelize
enum class BittensorReplaceOrigin : Parcelable {
    SUBNET_AMOUNT, ROOT_STAKE
}
