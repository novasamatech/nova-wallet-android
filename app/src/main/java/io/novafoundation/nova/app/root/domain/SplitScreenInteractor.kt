package io.novafoundation.nova.app.root.domain

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_dapp_api.data.model.SimpleTabModel
import io.novafoundation.nova.feature_dapp_api.data.repository.BrowserTabExternalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest

class SplitScreenInteractor(
    private val repository: BrowserTabExternalRepository,
    private val accountRepository: AccountRepository
) {

    fun observeTabNamesById(): Flow<List<SimpleTabModel>> {
        return accountRepository.selectedMetaAccountFlow()
            .flatMapLatest { repository.observeTabsWithNames(it.id) }
    }

    suspend fun removeAllTabs() {
        val metaAccount = accountRepository.getSelectedMetaAccount()
        repository.removeTabsForMetaAccount(metaAccount.id)
    }
}
