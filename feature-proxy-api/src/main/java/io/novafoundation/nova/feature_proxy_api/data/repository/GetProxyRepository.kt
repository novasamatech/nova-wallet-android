package io.novafoundation.nova.feature_proxy_api.data.repository

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_proxy_api.data.model.ProxiedWithProxy
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyType
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import java.math.BigInteger
import jp.co.soramitsu.fearless_utils.runtime.AccountId

interface GetProxyRepository {

    suspend fun getAllProxiesForAccounts(chainId: ChainId, accountIds: Set<AccountIdKey>): List<ProxiedWithProxy>

    suspend fun getDelegatedProxyTypes(chainId: ChainId, proxiedAccountId: AccountId, proxyAccountId: AccountId): List<ProxyType>

    suspend fun getProxiesQuantity(chainId: ChainId, proxiedAccountId: AccountId): Int

    suspend fun getProxyDeposit(chainId: ChainId, proxiedAccountId: AccountId): BigInteger

    suspend fun maxProxiesQuantity(chain: Chain): Int
}
