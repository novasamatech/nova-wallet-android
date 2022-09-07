package io.novafoundation.nova.feature_account_impl.domain

import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.common.utils.amountFromPlanks
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.utils.sumByBigDecimal
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.MetaAccountGroupingInteractor
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccountAssetBalance
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccountWithTotalBalance
import io.novafoundation.nova.feature_account_api.domain.model.hasAccountIn
import io.novafoundation.nova.feature_currency_api.domain.interfaces.CurrencyRepository
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class MetaAccountGroupingInteractorImpl(
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val currencyRepository: CurrencyRepository,
) : MetaAccountGroupingInteractor {

    override fun metaAccountsWithTotalBalanceFlow(): Flow<GroupedList<LightMetaAccount.Type, MetaAccountWithTotalBalance>> {
        return combine(
            currencyRepository.observeSelectCurrency(),
            accountRepository.allMetaAccountsFlow(),
            accountRepository.metaAccountBalancesFlow()
        ) { selectedCurrency, accounts, allBalances ->
            val groupedBalances = allBalances.groupBy(MetaAccountAssetBalance::metaId)

            accounts.map { metaAccount ->
                val accountBalances = groupedBalances[metaAccount.id] ?: emptyList()

                val totalBalance = accountBalances.sumByBigDecimal {
                    val totalInPlanks = it.freeInPlanks + it.reservedInPlanks

                    totalInPlanks.amountFromPlanks(it.precision) * it.rate.orZero()
                }

                MetaAccountWithTotalBalance(
                    metaAccount = metaAccount,
                    totalBalance = totalBalance,
                    currency = selectedCurrency
                )
            }
                .groupBy { it.metaAccount.type }
                .toSortedMap(metaAccountTypeComparator())
        }
    }

    override fun getControlledMetaAccountsFlow(): Flow<GroupedList<LightMetaAccount.Type, MetaAccount>> = flowOf {
        getControlledMetaAccounts()
            .groupBy(MetaAccount::type)
            .toSortedMap(metaAccountTypeComparator())
    }

    override suspend fun hasAvailableMetaAccountsForDestination(chainId: ChainId): Boolean {
        val destinationChain = chainRegistry.getChain(chainId)
        return getControlledMetaAccounts()
            .any { it.hasAccountIn(destinationChain) }
    }

    private suspend fun getControlledMetaAccounts(): List<MetaAccount> {
        return accountRepository.allMetaAccounts()
            .filter { it.type != LightMetaAccount.Type.WATCH_ONLY }
    }

    private fun metaAccountTypeComparator() = compareBy<LightMetaAccount.Type> {
        when (it) {
            LightMetaAccount.Type.SECRETS -> 0
            LightMetaAccount.Type.PARITY_SIGNER -> 1
            LightMetaAccount.Type.LEDGER -> 2
            LightMetaAccount.Type.WATCH_ONLY -> 3
        }
    }
}
