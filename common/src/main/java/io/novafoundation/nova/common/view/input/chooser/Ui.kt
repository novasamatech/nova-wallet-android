package io.novafoundation.nova.common.view.input.chooser

import io.novafoundation.nova.common.base.BaseFragmentMixin

fun <T> BaseFragmentMixin<*>.setupListChooserMixin(
    listChooserMixin: ListChooserMixin<T>,
    view: ListChooserView
) {
    listChooserMixin.selectedOption.observe(view::setModel)

    listChooserMixin.chooseNewOption.awaitableActionLiveData.observeEvent { action ->
        ListChooserBottomSheet(
            context = fragment.requireContext(),
            payload = action.payload,
            onCancel = action.onCancel,
            onClicked = { _, item -> action.onSuccess(item) },
        ).show()
    }

    view.setOnClickListener {
        listChooserMixin.selectorClicked()
    }
}

fun <T> BaseFragmentMixin<*>.setupListChooserMixinBottomSheet(
    listChooserMixin: ListChooserMixin<T>
) {
    listChooserMixin.chooseNewOption.awaitableActionLiveData.observeEvent { action ->
        ListChooserBottomSheet(
            context = fragment.requireContext(),
            payload = action.payload,
            onCancel = action.onCancel,
            onClicked = { _, item -> action.onSuccess(item) },
        ).show()
    }
}
