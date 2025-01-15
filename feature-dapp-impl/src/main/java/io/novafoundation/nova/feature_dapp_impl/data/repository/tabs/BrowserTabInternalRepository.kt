package io.novafoundation.nova.feature_dapp_impl.data.repository.tabs

import io.novafoundation.nova.feature_dapp_api.data.repository.BrowserTabExternalRepository
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.BrowserTab
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.PageSnapshot
import kotlinx.coroutines.flow.Flow

interface BrowserTabInternalRepository : BrowserTabExternalRepository {

    suspend fun saveTab(tab: BrowserTab)

    suspend fun removeTab(tabId: String)

    suspend fun savePageSnapshot(tabId: String, snapshot: PageSnapshot)

    fun observeTabs(metaId: Long): Flow<List<BrowserTab>>

    suspend fun changeCurrentUrl(tabId: String, url: String)

    suspend fun changeKnownDAppMetadata(tabId: String, dappIconUrl: String?)
}
