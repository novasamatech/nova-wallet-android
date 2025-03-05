package io.novafoundation.nova.feature_dapp_impl.utils.tabs.models

class TabsState(
    val tabs: List<BrowserTab>,
    val selectedTab: CurrentTabState
) {

    fun stateId(): String {
        return tabs.joinToString { it.id } + "_" + selectedTab.stateId()
    }
}
