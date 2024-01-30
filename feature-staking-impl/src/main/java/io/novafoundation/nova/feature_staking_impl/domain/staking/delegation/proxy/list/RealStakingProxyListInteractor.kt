package io.novafoundation.nova.feature_staking_impl.domain.staking.delegation.proxy.list

import io.novafoundation.nova.feature_account_api.domain.account.identity.IdentityProvider
import io.novafoundation.nova.feature_proxy_api.data.repository.GetProxyRepository
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyType
import io.novafoundation.nova.feature_staking_impl.domain.staking.delegation.proxy.list.model.StakingProxyAccount
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface StakingProxyListInteractor {
    fun stakingProxyListFlow(chain: Chain, accountId: AccountId): Flow<List<StakingProxyAccount>>
}

class RealStakingProxyListInteractor(
    val getProxyRepository: GetProxyRepository,
    val identityProvider: IdentityProvider
) : StakingProxyListInteractor {

    override fun stakingProxyListFlow(chain: Chain, accountId: AccountId): Flow<List<StakingProxyAccount>> {
        return getProxyRepository.proxiesByTypeFlow(chain, accountId, ProxyType.Staking)
            .map { proxies ->
                val proxiesAccountIds = proxies.map { it.proxyAccountId.value }
                val proxyIdentities = identityProvider.identitiesFor(proxiesAccountIds, chain.id)
                proxies.map { proxy ->
                    val proxyAccountId = proxy.proxyAccountId
                    val identity = proxyIdentities[proxyAccountId]
                    StakingProxyAccount(
                        identity?.name ?: chain.addressOf(proxyAccountId.value),
                        proxyAccountId.value
                    )
                }
            }
    }
}
