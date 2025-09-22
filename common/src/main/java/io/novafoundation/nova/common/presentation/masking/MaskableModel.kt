package io.novafoundation.nova.common.presentation.masking

import io.novafoundation.nova.common.data.model.MaskingMode

sealed interface MaskableModel<out T> {
    data object Hidden : MaskableModel<Nothing>
    data class Unmasked<T>(val value: T) : MaskableModel<T>
}

inline fun <T> MaskingMode.toMaskableModel(valueReceiver: () -> T): MaskableModel<T> {
    return when (this) {
        MaskingMode.ENABLED -> MaskableModel.Hidden
        MaskingMode.DISABLED -> MaskableModel.Unmasked(valueReceiver())
    }
}

fun <T, R> MaskableModel<T>.map(mapper: (T) -> R): MaskableModel<R> = when (this) {
    is MaskableModel.Hidden -> MaskableModel.Hidden
    is MaskableModel.Unmasked -> MaskableModel.Unmasked(mapper(value))
}

fun <T> MaskableModel<T>.getUnmaskedOrElse(mapper: () -> T): T = when (this) {
    is MaskableModel.Hidden -> mapper()
    is MaskableModel.Unmasked -> value
}

fun <T> MaskableModel<T>.dataOrNull(): T? = when (this) {
    is MaskableModel.Hidden -> null
    is MaskableModel.Unmasked -> value
}

fun <T> MaskableModel<T>.onHidden(onHidden: () -> Unit): MaskableModel<T> {
    if (this is MaskableModel.Hidden) {
        onHidden()
    }
    return this
}

fun <T> MaskableModel<T>.onUnmasked(onUnmasked: (T) -> Unit): MaskableModel<T> {
    if (this is MaskableModel.Unmasked) {
        onUnmasked(this.value)
    }
    return this
}
