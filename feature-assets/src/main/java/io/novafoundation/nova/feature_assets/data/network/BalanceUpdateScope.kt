package io.novafoundation.nova.feature_assets.data.network

import io.novafoundation.nova.core.updater.UpdateScope
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BalanceUpdateScope(
    private val accountRepository: AccountRepository
) : UpdateScope<List<MetaAccount>> {

    override fun invalidationFlow(): Flow<List<MetaAccount>> {
        return accountRepository.selectedMetaAccountFlow()
            .map {
                if (it.type == LightMetaAccount.Type.PROXIED) {
                    getAccountsForProxied(it)
                } else {
                    listOf(it)
                }
            }
    }

    private suspend fun getAccountsForProxied(metaAccount: MetaAccount): List<MetaAccount> {
        val finalProxy = accountRepository.getFinalProxyForAccountOrNull(metaAccount.id)

        return buildList {
            add(metaAccount)
            if (finalProxy != null) {
                add(finalProxy)
            }
        }
    }

}
