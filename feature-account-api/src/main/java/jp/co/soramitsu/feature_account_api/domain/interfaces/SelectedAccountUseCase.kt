package jp.co.soramitsu.feature_account_api.domain.interfaces

import jp.co.soramitsu.feature_account_api.domain.model.MetaAccount
import kotlinx.coroutines.flow.Flow

class SelectedAccountUseCase(
    private val accountRepository: AccountRepository
) {

    fun selectedMetaAccountFlow(): Flow<MetaAccount> = accountRepository.selectedMetaAccountFlow()
}
