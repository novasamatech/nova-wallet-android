package io.novafoundation.nova.feature_swap_impl.data.history

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.Filter
import io.novafoundation.nova.feature_account_api.domain.account.system.AccountSystemAccountMatcher
import io.novafoundation.nova.feature_account_api.domain.account.system.CompoundSystemAccountMatcher
import io.novafoundation.nova.feature_account_api.domain.account.system.SystemAccountMatcher
import io.novafoundation.nova.feature_swap_api.data.history.SwapTransferFilterFactory
import io.novafoundation.nova.feature_swap_api.domain.model.HydrationSystemAccounts
import io.novafoundation.nova.feature_swap_api.domain.model.NovaSwapCommission
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation
import io.novafoundation.nova.runtime.ext.accountIdOrNull
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.assetConversionSupported
import io.novafoundation.nova.runtime.ext.hydraDxSupported
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import javax.inject.Inject

@FeatureScope
class RealSwapTransferFilterFactory @Inject constructor(
    private val novaSwapCommission: NovaSwapCommission,
    private val hydrationSystemAccounts: HydrationSystemAccounts,
) : SwapTransferFilterFactory {

    override fun create(chain: Chain): Filter<Operation>? {
        val supportsHydration = chain.swap.hydraDxSupported()
        val assetHubFeeAccountId = if (chain.swap.assetConversionSupported()) {
            novaSwapCommission.assetHubFeeAccountId(chain.id)
        } else {
            null
        }
        if (!supportsHydration && assetHubFeeAccountId == null) return null

        val systemAccounts = buildList {
            if (supportsHydration) {
                add(AccountSystemAccountMatcher(AccountIdKey(novaSwapCommission.hydrationFeeAccountId)))
                add(AccountSystemAccountMatcher(AccountIdKey(hydrationSystemAccounts.routerAccountId)))
            }

            if (assetHubFeeAccountId != null) {
                add(AccountSystemAccountMatcher(AccountIdKey(assetHubFeeAccountId)))
            }
        }

        return IgnoreTransfersInvolvingSystemAccount(CompoundSystemAccountMatcher(systemAccounts), chain)
    }

    override fun commissionBeneficiaryAddresses(chain: Chain): Set<String> {
        return buildSet {
            if (chain.swap.hydraDxSupported()) {
                add(chain.addressOf(novaSwapCommission.hydrationFeeAccountId))
            }

            if (chain.swap.assetConversionSupported()) {
                novaSwapCommission.assetHubFeeAccountId(chain.id)?.let { add(chain.addressOf(it)) }
            }
        }
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
