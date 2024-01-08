package io.novafoundation.nova.feature_account_api.data.repository.addAccount.proxied

import io.novafoundation.nova.feature_proxy_api.data.model.ProxiedWithProxy
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.AddAccountRepository
import io.novafoundation.nova.feature_account_api.domain.account.identity.Identity

interface ProxiedAddAccountRepository : AddAccountRepository<ProxiedAddAccountRepository.Payload> {

    class Payload(
        val proxiedWithProxy: ProxiedWithProxy,
        val proxyMetaId: Long,
        val identity: Identity?
    )
}
