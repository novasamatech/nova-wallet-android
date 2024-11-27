package io.novafoundation.nova.feature_dapp_api.data.repository

import kotlinx.coroutines.flow.Flow

interface BrowserTabExternalRepository {

    fun observeTabsWithNames(): Flow<Map<String, String?>>

    suspend fun removeAllTabs()
}
