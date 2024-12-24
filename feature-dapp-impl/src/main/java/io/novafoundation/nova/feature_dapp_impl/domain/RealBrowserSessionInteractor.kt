package io.novafoundation.nova.feature_dapp_impl.domain

import io.novafoundation.nova.common.utils.skipFirst
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_dapp_api.domain.BrowserSessionInteractor
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.BrowserTabService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class RealBrowserSessionInteractor(
    private val browserTabService: BrowserTabService,
    private val accountRepository: AccountRepository
) : BrowserSessionInteractor {

    override fun destroyActiveSessionsOnAccountChange(): Flow<Unit> {
        return accountRepository.selectedMetaAccountFlow()
            .skipFirst() // Skip first to not trigger when app starts
            .distinctUntilChangedBy { it.id }
            .onEach { browserTabService.destroyActiveSessions() }
            .map { Unit }
    }
}
