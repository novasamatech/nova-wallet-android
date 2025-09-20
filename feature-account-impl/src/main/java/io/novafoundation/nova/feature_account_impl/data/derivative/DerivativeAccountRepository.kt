package io.novafoundation.nova.feature_account_impl.data.derivative

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_impl.data.derivative.model.DiscoveredDerivativeAccount
import io.novafoundation.nova.runtime.ext.toSubstrateAccountIdKey
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import javax.inject.Inject

interface DerivativeAccountRepository {

    fun areDerivativeAccountsSupported(chain: Chain): Boolean

    suspend fun getDerivativeAccounts(accountIdsToQuery: Set<AccountIdKey>): List<DiscoveredDerivativeAccount>
}

@FeatureScope
class RealDerivativeAccountRepository @Inject constructor(
): DerivativeAccountRepository {

    private val hardcoded = mapOf(
        "15UHvPeMjYLvMLqh6bWLxAP3MbqjjsMXFWToJKCijzGPM3p9".toSubstrateAccountIdKey() to listOf(
            DiscoveredDerivativeAccount(
                parent = "15UHvPeMjYLvMLqh6bWLxAP3MbqjjsMXFWToJKCijzGPM3p9".toSubstrateAccountIdKey(),
                derivative = "127zarPDhVzmCXVQ7Kfr1yyaa9wsMuJ74GJW9Q7ezHfQEgh6".toSubstrateAccountIdKey(),
                index = 0
            ),
        )
    )

    override fun areDerivativeAccountsSupported(chain: Chain): Boolean {
        // TODO derivative: separate feature flag
        return chain.hasSubstrateRuntime
    }

    override suspend fun getDerivativeAccounts(accountIdsToQuery: Set<AccountIdKey>): List<DiscoveredDerivativeAccount> {
       return accountIdsToQuery.flatMap { hardcoded[it].orEmpty() }
    }
}
