package io.novafoundation.nova.feature_account_api.domain.updaters

import io.novafoundation.nova.core.updater.UpdateScope
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import kotlinx.coroutines.flow.Flow

class AccountUpdateScope(
    private val accountRepository: AccountRepository
) : UpdateScope<MetaAccount> {

    override fun invalidationFlow(): Flow<MetaAccount> {
        return accountRepository.selectedMetaAccountFlow()
    }
}
