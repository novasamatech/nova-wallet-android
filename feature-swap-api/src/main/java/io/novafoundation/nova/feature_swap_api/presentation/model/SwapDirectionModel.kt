package io.novafoundation.nova.feature_swap_api.presentation.model

import android.os.Parcelable
import io.novafoundation.nova.feature_swap_core.domain.model.SwapDirection
import kotlinx.parcelize.Parcelize

@Parcelize
enum class SwapDirectionModel : Parcelable {
    SPECIFIED_IN,
    SPECIFIED_OUT
}

fun SwapDirectionModel.mapFromModel(): SwapDirection {
    return when (this) {
        SwapDirectionModel.SPECIFIED_IN -> SwapDirection.SPECIFIED_IN
        SwapDirectionModel.SPECIFIED_OUT -> SwapDirection.SPECIFIED_OUT
    }
}

fun SwapDirection.mapToModel(): SwapDirectionModel {
    return when (this) {
        SwapDirection.SPECIFIED_IN -> SwapDirectionModel.SPECIFIED_IN
        SwapDirection.SPECIFIED_OUT -> SwapDirectionModel.SPECIFIED_OUT
    }
}
