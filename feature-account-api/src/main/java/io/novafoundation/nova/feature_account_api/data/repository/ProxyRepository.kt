package io.novafoundation.nova.feature_account_api.data.repository

import io.novafoundation.nova.feature_account_api.data.model.ProxiedWithProxies
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccountId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface ProxyRepository {
    
    suspend fun getProxyDelegatorsForAccounts(chainId: ChainId, metaAccountIds: List<MetaAccountId>): List<ProxiedWithProxies>
}
