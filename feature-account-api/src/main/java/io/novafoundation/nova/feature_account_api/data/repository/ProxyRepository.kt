package io.novafoundation.nova.feature_account_api.data.repository

import io.novafoundation.nova.feature_account_api.data.model.ProxiedWithProxy
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccountId
import io.novafoundation.nova.feature_account_api.domain.model.ProxyAccount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.runtime.AccountId

interface ProxyRepository {

    suspend fun getProxyDelegatorsForAccounts(chainId: ChainId, metaAccountIds: List<MetaAccountId>): List<ProxiedWithProxy>

    suspend fun getDelegatedProxyTypes(chainId: ChainId, proxiedAccountId: AccountId, proxyAccountId: AccountId): List<ProxyAccount.ProxyType>
}
