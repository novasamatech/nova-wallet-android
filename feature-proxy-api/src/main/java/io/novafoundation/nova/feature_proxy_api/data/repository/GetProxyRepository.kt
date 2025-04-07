package io.novafoundation.nova.feature_proxy_api.data.repository

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_proxy_api.data.common.NestedProxiesGraphConstructor
import io.novafoundation.nova.feature_proxy_api.data.model.ProxyPermission
import io.novafoundation.nova.feature_proxy_api.data.model.RelaychainRemoteProxyProof
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyType
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import java.math.BigInteger
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.flow.Flow

interface GetProxyRepository {

    suspend fun findAllProxiedsForAccounts(chainId: ChainId, accountIds: Set<AccountIdKey>): List<NestedProxiesGraphConstructor.Node>

    suspend fun getDelegatedProxyTypesRemote(chainId: ChainId, proxiedAccountId: AccountId, proxyAccountId: AccountId): List<ProxyType>

    suspend fun getDelegatedProxyTypesLocal(chainId: ChainId, proxiedAccountId: AccountId, proxyAccountId: AccountId): List<ProxyType>

    suspend fun getProxiesQuantity(chainId: ChainId, proxiedAccountId: AccountId): Int

    suspend fun getProxyDeposit(chainId: ChainId, proxiedAccountId: AccountId): BigInteger

    suspend fun maxProxiesQuantity(chain: Chain): Int

    fun proxiesByTypeFlow(chain: Chain, accountId: AccountId, proxyType: ProxyType): Flow<List<ProxyPermission>>

    fun proxiesQuantityByTypeFlow(chain: Chain, accountId: AccountId, proxyType: ProxyType): Flow<Int>

    suspend fun getRelaychainProxyProof(
        mainProxyChainId: ChainId,
        remoteProxyChainId: ChainId,
        proxiedAccountId: AccountId
    ): RelaychainRemoteProxyProof
}
