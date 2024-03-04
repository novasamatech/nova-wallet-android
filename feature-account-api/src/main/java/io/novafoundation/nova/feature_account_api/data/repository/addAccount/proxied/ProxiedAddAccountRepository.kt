package io.novafoundation.nova.feature_account_api.data.repository.addAccount.proxied

import io.novafoundation.nova.feature_account_api.data.repository.addAccount.AddAccountRepository
import io.novafoundation.nova.feature_account_api.domain.account.identity.Identity
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyType
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.runtime.AccountId

interface ProxiedAddAccountRepository : AddAccountRepository<ProxiedAddAccountRepository.Payload> {

    class Payload(
        val chainId: ChainId,
        val proxiedAccountId: AccountId,
        val proxyType: ProxyType,
        val proxyMetaId: Long,
        val identity: Identity?
    )
}
