package io.novafoundation.nova.common.base

fun BaseViewModel.showError(model: TitleAndMessage) {
    if (model.second != null) {
        showError(model.first, model.second!!)
    } else {
        showError(model.first)
    }
}
