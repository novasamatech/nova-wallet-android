package io.novafoundation.nova.feature_swap_impl.data.history

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.Filter
import io.novafoundation.nova.feature_account_api.domain.account.system.AccountSystemAccountMatcher
import io.novafoundation.nova.feature_account_api.domain.account.system.CompoundSystemAccountMatcher
import io.novafoundation.nova.feature_account_api.domain.account.system.SystemAccountMatcher
import io.novafoundation.nova.feature_swap_api.data.history.HydrationSwapTransferFilterFactory
import io.novafoundation.nova.feature_swap_api.domain.model.HydrationSystemAccounts
import io.novafoundation.nova.feature_swap_api.domain.model.NovaSwapCommission
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation
import io.novafoundation.nova.runtime.ext.accountIdOrNull
import io.novafoundation.nova.runtime.ext.hydraDxSupported
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import javax.inject.Inject

@FeatureScope
class RealHydrationSwapTransferFilterFactory @Inject constructor() : HydrationSwapTransferFilterFactory {

    override fun create(chain: Chain): Filter<Operation>? {
        if (!chain.swap.hydraDxSupported()) return null

        val matcher = CompoundSystemAccountMatcher(
            AccountSystemAccountMatcher(AccountIdKey(NovaSwapCommission.feeAccountId)),
            AccountSystemAccountMatcher(AccountIdKey(HydrationSystemAccounts.routerAccountId)),
        )
        return IgnoreTransfersInvolvingSystemAccount(matcher, chain)
    }

    private class IgnoreTransfersInvolvingSystemAccount(
        private val systemAccountMatcher: SystemAccountMatcher,
        private val chain: Chain,
    ) : Filter<Operation> {

        override fun shouldInclude(model: Operation): Boolean {
            val operationType = model.type as? Operation.Type.Transfer ?: return true

            return !chain.matchesSystemAccount(operationType.sender) &&
                !chain.matchesSystemAccount(operationType.receiver)
        }

        private fun Chain.matchesSystemAccount(address: String): Boolean {
            val accountId = accountIdOrNull(address) ?: return false
            return systemAccountMatcher.isSystemAccount(accountId)
        }
    }
}
