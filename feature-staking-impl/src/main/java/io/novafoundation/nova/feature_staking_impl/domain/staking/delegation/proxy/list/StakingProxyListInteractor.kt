package io.novafoundation.nova.feature_staking_impl.domain.staking.delegation.proxy.list

import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.hasAccountIn
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_proxy_api.data.repository.GetProxyRepository
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyType
import io.novafoundation.nova.feature_staking_impl.domain.staking.delegation.proxy.list.model.StakingProxyAccount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class StakingProxyListInteractor(
    val accountRepository: AccountRepository,
    val getProxyRepository: GetProxyRepository
) {

    fun stakingProxyListFlow(chain: Chain, accountId: AccountId): Flow<List<StakingProxyAccount>> {
        return combine(
            accountRepository.activeMetaAccountsFlow(),
            getProxyRepository.proxiesByTypeFlow(chain, accountId, ProxyType.Staking)
        ) { metaAccounts, proxies ->
            val accountIdToMetaAccount = metaAccounts
                .filter { it.hasAccountIn(chain) }
                .associateBy {
                    it.requireAccountIdIn(chain).intoKey()
                }

            proxies.map { proxy ->
                val proxyAccountId = proxy.accountId
                StakingProxyAccount(
                    accountIdToMetaAccount[proxyAccountId],
                    proxyAccountId.value
                )
            }
        }
    }
}
