package io.novafoundation.nova.feature_dapp_impl.presentation.browser.tabPool

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

interface TabPoolService {

    fun currentTabFlow(): Flow<TabState>

    fun selectTab(tab: TabState)

    fun getAllTabs(): List<TabState>

    fun getCurrentTab(): TabState

    fun setSnapshotForCurrentTab(path: String)
}

class RealTabPoolService(
    private val context: Context
) : TabPoolService {

    private val allTabs = mutableListOf<TabState>(
        TabState("1", RealPageSession(context, "https://google.com"), null),
        TabState("2", RealPageSession(context, "https://www.youtube.com"), null),
        TabState("3", RealPageSession(context, "https://translate.google.com/"), null),
        TabState("4", RealPageSession(context, "https://www.notion.so"), null)
    )
    private val currentTab = MutableStateFlow<TabState>(allTabs.first())

    override fun currentTabFlow(): Flow<TabState> {
        return currentTab
    }
///storage/emulated/0/Android/data/io.novafoundation.nova.debug/files/TabsScreenshots/4e7d2f24-034f-4efe-8e8d-da4b2dc45094.png
    override fun selectTab(tab: TabState) {
        currentTab.value = tab
    }

    override fun getAllTabs(): List<TabState> {
        return allTabs
    }

    override fun getCurrentTab(): TabState {
        return currentTab.value
    }

    override fun setSnapshotForCurrentTab(path: String) {
        currentTab.value.previewImagePath = path
    }
}
