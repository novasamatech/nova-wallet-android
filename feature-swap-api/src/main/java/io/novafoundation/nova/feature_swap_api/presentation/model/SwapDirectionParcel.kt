package io.novafoundation.nova.feature_swap_api.presentation.model

import android.os.Parcelable
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import kotlinx.android.parcel.Parcelize

@Parcelize
enum class SwapDirectionParcel : Parcelable {
    SPECIFIED_IN,
    SPECIFIED_OUT
}

fun SwapDirectionParcel.mapFromModel(): SwapDirection {
    return when (this) {
        SwapDirectionParcel.SPECIFIED_IN -> SwapDirection.SPECIFIED_IN
        SwapDirectionParcel.SPECIFIED_OUT -> SwapDirection.SPECIFIED_OUT
    }
}

fun SwapDirection.toParcel(): SwapDirectionParcel {
    return when (this) {
        SwapDirection.SPECIFIED_IN -> SwapDirectionParcel.SPECIFIED_IN
        SwapDirection.SPECIFIED_OUT -> SwapDirectionParcel.SPECIFIED_OUT
    }
}
