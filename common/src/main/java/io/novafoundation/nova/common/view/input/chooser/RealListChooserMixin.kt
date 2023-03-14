package io.novafoundation.nova.common.view.input.chooser

import androidx.annotation.StringRes
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.common.view.input.chooser.ListChooserMixin.Model
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal class RealListChooserMixinFactory(
    private val actionAwaitableMixin: ActionAwaitableMixin.Factory
) : ListChooserMixin.Factory {

    override fun <T> create(
        coroutineScope: CoroutineScope,
        dataProvider: ListChooserDataProvider<T>,
        selectorTitleRes: Int,
    ): ListChooserMixin<T> {
        return RealListChooserMixin(coroutineScope, actionAwaitableMixin, dataProvider, selectorTitleRes)
    }
}

private class RealListChooserMixin<T>(
    coroutineScope: CoroutineScope,
    actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val dataProvider: ListChooserDataProvider<T>,
    @StringRes private val selectorTitleRes: Int,
) : ListChooserMixin<T>, CoroutineScope by coroutineScope {

    override val chooseNewOption = actionAwaitableMixinFactory.create<ListChooserBottomSheet.Payload<Model<T>>, Model<T>>()

    override val selectedOption = singleReplaySharedFlow<Model<T>>()

    private val data = async(Dispatchers.Default) { dataProvider() }

    init {
        launch {
            updateSelectedOption(data.await().initial)
        }
    }

    override fun selectorClicked() {
        launch {
            val allOptions = data.await().all
            val currentlySelected = selectedOption.first()

            val payload = ListChooserBottomSheet.Payload(allOptions, currentlySelected, selectorTitleRes)

            val newSelectedOption = chooseNewOption.awaitAction(payload)
            updateSelectedOption(newSelectedOption)
        }
    }

    suspend fun updateSelectedOption(model: Model<T>) {
        selectedOption.emit(model)
    }
}
