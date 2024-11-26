package io.novafoundation.nova.app.root.domain

import io.novafoundation.nova.app.root.data.browser.TabsRepository
import kotlinx.coroutines.flow.Flow

class SplitScreenInteractor(val repository: TabsRepository) {

    fun observeTabIds(): Flow<List<String>> {
        return repository.observeTabIds()
    }

    suspend fun removeAllTabs() {
        repository.removeAllTabs()
    }
}
