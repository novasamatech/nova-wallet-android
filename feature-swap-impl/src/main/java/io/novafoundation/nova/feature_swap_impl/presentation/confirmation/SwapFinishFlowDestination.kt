package io.novafoundation.nova.feature_swap_impl.presentation.confirmation

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
enum class SwapFinishFlowDestination : Parcelable {
    BALANCE_LIST,
    BALANCE_DETAILS
}
