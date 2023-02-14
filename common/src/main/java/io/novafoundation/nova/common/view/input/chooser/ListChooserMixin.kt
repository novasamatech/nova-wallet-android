package io.novafoundation.nova.common.view.input.chooser

import androidx.annotation.StringRes
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

typealias ListChooserDataProvider<T> = suspend () -> ListChooserMixin.Data<T>
typealias ListChooserAwaitable<E> = ActionAwaitableMixin<ListChooserBottomSheet.Payload<E>, E>

interface ListChooserMixin<T> {

    interface Factory {

        fun <T> create(
            coroutineScope: CoroutineScope,
            dataProvider: ListChooserDataProvider<T>,
            @StringRes selectorTitleRes: Int,
        ): ListChooserMixin<T>
    }

    class Data<T>(val all: List<Model<T>>, val initial: Model<T>)

    class Model<T>(val value: T, val display: String)

    val chooseNewOption: ListChooserAwaitable<Model<T>>

    val selectedOption: Flow<Model<T>>

    fun selectorClicked()
}

val <T> ListChooserMixin<T>.selectedValue: Flow<T>
    get() = selectedOption.map { it.value }

inline fun <reified E : Enum<E>> ListChooserMixin.Factory.createFromEnum(
    coroutineScope: CoroutineScope,
    noinline displayOf: suspend (E) -> String,
    initial: E,
    selectorTitleRes: Int
): ListChooserMixin<E> {
    val provider = suspend {
        val all = enumValues<E>().map { enumValue ->
            ListChooserMixin.Model(enumValue, displayOf(enumValue))
        }
        val initialModel = all.first { it.value == initial }

        ListChooserMixin.Data(all, initialModel)
    }

    return create(coroutineScope, provider, selectorTitleRes)
}
