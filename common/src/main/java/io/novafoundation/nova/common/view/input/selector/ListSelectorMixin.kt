package io.novafoundation.nova.common.view.input.selector

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.utils.Event
import kotlinx.coroutines.CoroutineScope

interface ListSelectorMixin {

    interface Factory {

        fun create(
            coroutineScope: CoroutineScope
        ): ListSelectorMixin
    }

    class Payload(@StringRes val titleRes: Int, val items: List<Item>)

    class Item(
        @DrawableRes val iconRes: Int,
        @ColorRes val iconTintRes: Int,
        @StringRes val titleRes: Int,
        @ColorRes val titleColorRes: Int,
        val onClick: () -> Unit
    )

    val actionLiveData: LiveData<Event<Payload>>

    fun showSelector(@StringRes titleRes: Int, items: List<Item>)
}

class RealListSelectorMixinFactory : ListSelectorMixin.Factory {

    override fun create(
        coroutineScope: CoroutineScope
    ): RealListSelectorMixin {
        return RealListSelectorMixin(coroutineScope)
    }
}

class RealListSelectorMixin(
    coroutineScope: CoroutineScope
) : ListSelectorMixin, CoroutineScope by coroutineScope {

    override val actionLiveData: MutableLiveData<Event<ListSelectorMixin.Payload>> = MutableLiveData()

    override fun showSelector(titleRes: Int, items: List<ListSelectorMixin.Item>) {
        actionLiveData.value = Event(ListSelectorMixin.Payload(titleRes, items))
    }
}
