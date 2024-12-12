package io.novafoundation.nova.feature_dapp_api.data.repository

import io.novafoundation.nova.feature_dapp_api.data.model.SimpleTabModel
import kotlinx.coroutines.flow.Flow

interface BrowserTabExternalRepository {

    fun observeTabsWithNames(): Flow<List<SimpleTabModel>>

    suspend fun removeAllTabs()
}
