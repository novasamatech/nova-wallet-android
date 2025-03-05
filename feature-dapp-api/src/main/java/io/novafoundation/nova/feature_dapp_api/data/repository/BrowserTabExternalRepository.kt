package io.novafoundation.nova.feature_dapp_api.data.repository

import io.novafoundation.nova.feature_dapp_api.data.model.SimpleTabModel
import kotlinx.coroutines.flow.Flow

interface BrowserTabExternalRepository {

    fun observeTabsWithNames(metaId: Long): Flow<List<SimpleTabModel>>

    suspend fun removeTabsForMetaAccount(metaId: Long): List<String>
}
