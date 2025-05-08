package io.novafoundation.nova.feature_pay_impl.domain

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ShopInteractor(
    private val accountRepository: AccountRepository
) {

    fun observeAccountAvailableForShopping(): Flow<Boolean> {
        return accountRepository.selectedMetaAccountFlow().map {
            it.type == LightMetaAccount.Type.SECRETS
        }
    }
}
