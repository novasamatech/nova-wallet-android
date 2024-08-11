package io.novafoundation.nova.feature_swap_api.presentation.model

import android.os.Parcelable
import io.novafoundation.nova.feature_swap_core.domain.model.SwapDirection
import kotlinx.android.parcel.Parcelize

@Parcelize
enum class SwapDirectionModel : Parcelable {
    SPECIFIED_IN,
    SPECIFIED_OUT
}

fun SwapDirectionModel.mapFromModel(): io.novafoundation.nova.feature_swap_core.domain.model.SwapDirection {
    return when (this) {
        SwapDirectionModel.SPECIFIED_IN -> io.novafoundation.nova.feature_swap_core.domain.model.SwapDirection.SPECIFIED_IN
        SwapDirectionModel.SPECIFIED_OUT -> io.novafoundation.nova.feature_swap_core.domain.model.SwapDirection.SPECIFIED_OUT
    }
}

fun io.novafoundation.nova.feature_swap_core.domain.model.SwapDirection.mapToModel(): SwapDirectionModel {
    return when (this) {
        io.novafoundation.nova.feature_swap_core.domain.model.SwapDirection.SPECIFIED_IN -> SwapDirectionModel.SPECIFIED_IN
        io.novafoundation.nova.feature_swap_core.domain.model.SwapDirection.SPECIFIED_OUT -> SwapDirectionModel.SPECIFIED_OUT
    }
}
