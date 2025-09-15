package io.novafoundation.nova.common.presentation.masking

import io.novafoundation.nova.common.data.model.DiscreetMode
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

sealed interface MaskableModel<out T> {
    class Hidden<T> : MaskableModel<T>
    class Unmasked<T>(val value: T) : MaskableModel<T>
}

fun <T> DiscreetMode.toMaskableModel(valueReceiver: () -> T): MaskableModel<T> {
    return when (this) {
        DiscreetMode.ENABLED -> MaskableModel.Hidden()
        DiscreetMode.DISABLED -> MaskableModel.Unmasked(valueReceiver())
    }
}

fun <T, R> MaskableModel<T>.map(mapper: (T) -> R): MaskableModel<R> = when (this) {
    is MaskableModel.Hidden -> MaskableModel.Hidden()
    is MaskableModel.Unmasked -> MaskableModel.Unmasked(mapper(value))
}

fun <T> MaskableModel<T>.unfoldHidden(mapper: () -> T): T = when (this) {
    is MaskableModel.Hidden -> mapper()
    is MaskableModel.Unmasked -> value
}

@OptIn(ExperimentalContracts::class)
fun <T> MaskableModel<T>.isUnmasked(): Boolean {
    contract {
        returns(true) implies (this@isUnmasked is MaskableModel.Unmasked<T>)
    }

    return this is MaskableModel.Unmasked
}

fun <T> MaskableModel<T>.dataOrNull(): T? = when (this) {
    is MaskableModel.Hidden -> null
    is MaskableModel.Unmasked -> value
}
