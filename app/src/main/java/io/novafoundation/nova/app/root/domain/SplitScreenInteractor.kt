package io.novafoundation.nova.app.root.domain

import io.novafoundation.nova.feature_dapp_api.data.repository.BrowserTabExternalRepository
import kotlinx.coroutines.flow.Flow

class SplitScreenInteractor(val repository: BrowserTabExternalRepository) {

    fun observeTabsWithNames(): Flow<Map<String, String?>> {
        return repository.observeTabsWithNames()
    }

    suspend fun removeAllTabs() {
        repository.removeAllTabs()
    }
}
