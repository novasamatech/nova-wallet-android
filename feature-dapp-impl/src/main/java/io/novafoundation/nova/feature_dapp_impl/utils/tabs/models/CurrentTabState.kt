package io.novafoundation.nova.feature_dapp_impl.utils.tabs.models

sealed interface CurrentTabState {

    object NotSelected : CurrentTabState

    data class Selected(val tab: BrowserTab, val pageSession: PageSession) : CurrentTabState
}

fun CurrentTabState.stateId() = when (this) {
    CurrentTabState.NotSelected -> "not_selected"
    is CurrentTabState.Selected -> tab.id
}
