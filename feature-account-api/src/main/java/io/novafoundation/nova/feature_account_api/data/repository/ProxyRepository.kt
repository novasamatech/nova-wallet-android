package io.novafoundation.nova.feature_account_api.data.repository

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface ProxyRepository {
    suspend fun getProxyDelegatorsForAccounts(chainId: ChainId, chainAccounts: List<MetaAccount.ChainAccount>)
}
