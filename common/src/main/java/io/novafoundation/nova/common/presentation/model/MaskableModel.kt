package io.novafoundation.nova.common.presentation.model

import io.novafoundation.nova.common.data.model.DiscreetMode

sealed interface MaskableModel<T> {
    class Hidden<T> : MaskableModel<T>
    class Unmasked<T>(val value: T) : MaskableModel<T>
}

fun <T> DiscreetMode.toMaskableModel(valueReceiver: () -> T): MaskableModel<T> {
    return when (this) {
        DiscreetMode.ENABLED -> MaskableModel.Hidden()
        DiscreetMode.DISABLED -> MaskableModel.Unmasked(valueReceiver())
    }
}
