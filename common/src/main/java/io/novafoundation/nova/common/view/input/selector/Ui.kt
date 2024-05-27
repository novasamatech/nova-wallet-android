package io.novafoundation.nova.common.view.input.selector

import io.novafoundation.nova.common.base.BaseFragmentMixin

fun BaseFragmentMixin<*>.setupListSelectorMixin(listChooserMixin: ListSelectorMixin) {
    listChooserMixin.actionLiveData.observeEvent { action ->
        DynamicSelectorBottomSheet(
            context = fragment.requireContext(),
            payload = DynamicSelectorBottomSheet.Payload(
                titleRes = action.titleRes,
                data = action.items
            ),
            onClicked = { _, item -> item.onClick() },
        ).show()
    }
}
