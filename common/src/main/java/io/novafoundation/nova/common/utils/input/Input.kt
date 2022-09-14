package io.novafoundation.nova.common.utils.input

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

sealed class Input<out I> {

    sealed class Enabled<I>(val value: I) : Input<I>() {
        class Modifiable<I>(value: I) : Enabled<I>(value)

        class UnModifiable<I>(value: I) : Enabled<I>(value)
    }

    object Disabled : Input<Nothing>()
}

fun <T> T.modifiableInput(): Input<T> = Input.Enabled.Modifiable(this)
fun <T> T.unmodifiableInput(): Input<T> = Input.Enabled.UnModifiable(this)
fun <T> disabledInput(): Input<T> = Input.Disabled

fun <I, R> Input<I>.map(modification: (I) -> R): Input<R> = when (this) {
    is Input.Enabled.Modifiable -> Input.Enabled.Modifiable(modification(value))
    is Input.Enabled.UnModifiable -> Input.Enabled.UnModifiable(modification(value))
    Input.Disabled -> Input.Disabled
}

fun <I> Input<I>.modify(new: I): Input<I> = when (this) {
    is Input.Enabled.Modifiable -> Input.Enabled.Modifiable(new)
    is Input.Enabled.UnModifiable -> this
    Input.Disabled -> this
}

fun <I> Input<I>.modifyIfNotNull(new: I?): Input<I> = new?.let { modify(it) } ?: this

fun <I, R> Input<I>.fold(
    ifEnabled: (I) -> R,
    ifDisabled: R
): R = when (this) {
    is Input.Enabled -> ifEnabled(value)
    Input.Disabled -> ifDisabled
}

inline fun <I> Input<I>.ifModifiable(action: (I) -> Unit) {
    (this as? Input.Enabled.Modifiable)?.let { action(it.value) }
}

val <I> Input<I>.valueOrNull
    get() = (this as? Input.Enabled)?.value

val Input<*>.isModifiable
    get() = this is Input.Enabled.Modifiable

suspend fun <I> MutableSharedFlow<Input<I>>.modifyInput(newValue: I) {
    emit(first().modify(newValue))
}

fun <I> MutableStateFlow<Input<I>>.modifyInput(newValue: I) {
    value = value.modify(newValue)
}
