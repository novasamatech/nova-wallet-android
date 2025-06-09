package io.novafoundation.nova.feature_account_impl.domain

import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.common.utils.Filter
import io.novafoundation.nova.common.utils.amountFromPlanks
import io.novafoundation.nova.common.utils.applyFilter
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.utils.sumByBigDecimal
import io.novafoundation.nova.feature_account_api.data.proxy.MetaAccountsUpdatesRegistry
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.MetaAccountGroupingInteractor
import io.novafoundation.nova.feature_account_api.domain.model.AccountDelegation
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccountAssetBalance
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccountListingItem
import io.novafoundation.nova.feature_account_api.domain.model.MultisigMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.ProxiedMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.metaAccountTypeComparator
import io.novafoundation.nova.feature_currency_api.domain.interfaces.CurrencyRepository
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull

class MetaAccountGroupingInteractorImpl(
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val currencyRepository: CurrencyRepository,
    private val metaAccountsUpdatesRegistry: MetaAccountsUpdatesRegistry,
) : MetaAccountGroupingInteractor {

    override fun metaAccountsWithTotalBalanceFlow(): Flow<GroupedList<LightMetaAccount.Type, MetaAccountListingItem>> {
        return combine(
            currencyRepository.observeSelectCurrency(),
            accountRepository.activeMetaAccountsFlow(),
            accountRepository.metaAccountBalancesFlow(),
            metaAccountsUpdatesRegistry.observeUpdates(),
            chainRegistry.chainsById
        ) { selectedCurrency, accounts, allBalances, updatedMetaAccounts, chains ->
            val groupedBalances = allBalances.groupBy(MetaAccountAssetBalance::metaId)

            accounts.mapNotNull { metaAccount ->
                val accountBalances = groupedBalances[metaAccount.id] ?: emptyList()
                val hasUpdates = updatedMetaAccounts.contains(metaAccount.id)
                metaAccountWithTotalBalance(accountBalances, metaAccount, accounts, selectedCurrency, chains, hasUpdates)
            }
                .groupBy { it.metaAccount.type }
                .toSortedMap(metaAccountTypeComparator())
        }
    }

    override fun metaAccountWithTotalBalanceFlow(metaId: Long): Flow<MetaAccountListingItem> {
        return combine(
            currencyRepository.observeSelectCurrency(),
            accountRepository.activeMetaAccountsFlow(),
            accountRepository.metaAccountFlow(metaId),
            accountRepository.metaAccountBalancesFlow(metaId),
            chainRegistry.chainsById
        ) { selectedCurrency, allMetaAccounts, metaAccount, metaAccountBalances, chains ->
            metaAccountWithTotalBalance(metaAccountBalances, metaAccount, allMetaAccounts, selectedCurrency, chains, false)
        }.filterNotNull()
    }

    override fun getMetaAccountsWithFilter(
        metaAccountFilter: Filter<MetaAccount>
    ): Flow<GroupedList<LightMetaAccount.Type, MetaAccount>> = flowOf {
        getValidMetaAccountsForTransaction(metaAccountFilter)
            .groupBy(MetaAccount::type)
            .toSortedMap(metaAccountTypeComparator())
    }

    override fun updatedDelegates(): Flow<GroupedList<LightMetaAccount.Status, AccountDelegation>> {
        return combine(
            metaAccountsUpdatesRegistry.observeUpdates(),
            accountRepository.allMetaAccountsFlow(),
            chainRegistry.chainsById
        ) { updatedMetaIds, metaAccounts, chainsById ->
            val metaById = metaAccounts.associateBy(MetaAccount::id)

            metaAccounts
                .filter { updatedMetaIds.contains(it.id) }
                .mapNotNull {
                    when (it) {
                        is ProxiedMetaAccount -> AccountDelegation.Proxy(
                            it,
                            metaById[it.proxy.proxyMetaId] ?: return@mapNotNull null,
                            chainsById[it.proxy.chainId] ?: return@mapNotNull null
                        )

                        is MultisigMetaAccount -> AccountDelegation.Multisig(
                            it,
                            metaById[it.signatoryMetaId] ?: return@mapNotNull null
                        )

                        else -> null
                    }
                }
                .groupBy { it.delegator.status }
                .toSortedMap(metaAccountStateComparator())
        }
    }

    override suspend fun hasAvailableMetaAccountsForChain(
        chainId: ChainId,
        metaAccountFilter: Filter<MetaAccount>
    ): Boolean {
        val chain = chainRegistry.getChain(chainId)
        return getValidMetaAccountsForTransaction(metaAccountFilter)
            .any { it.hasAccountIn(chain) }
    }

    private fun metaAccountWithTotalBalance(
        metaAccountBalances: List<MetaAccountAssetBalance>,
        metaAccount: MetaAccount,
        allMetaAccounts: List<MetaAccount>,
        selectedCurrency: Currency,
        chains: Map<ChainId, Chain>,
        hasUpdates: Boolean
    ): MetaAccountListingItem? {
        val totalBalance = metaAccountBalances.sumByBigDecimal {
            val totalInPlanks = it.freeInPlanks + it.reservedInPlanks + it.offChainBalance.orZero()

            totalInPlanks.amountFromPlanks(it.precision) * it.rate.orZero()
        }

        return when {
            metaAccount is ProxiedMetaAccount -> {
                val proxyMetaAccount = allMetaAccounts.firstOrNull { it.id == metaAccount.proxy.proxyMetaId } ?: return null
                val proxyChain = metaAccount.proxy.chainId.let(chains::getValue)

                MetaAccountListingItem.Proxied(
                    proxyMetaAccount = proxyMetaAccount,
                    proxyChain = proxyChain,
                    metaAccount = metaAccount,
                    hasUpdates = hasUpdates,
                    totalBalance = totalBalance,
                    currency = selectedCurrency
                )
            }

            metaAccount is MultisigMetaAccount -> {
                val signatoryMetaAccount = allMetaAccounts.firstOrNull { it.id == metaAccount.signatoryMetaId } ?: return null

                MetaAccountListingItem.Multisig(
                    signatory = signatoryMetaAccount,
                    metaAccount = metaAccount,
                    hasUpdates = hasUpdates,
                    totalBalance = totalBalance,
                    currency = selectedCurrency
                )
            }

            else -> {
                MetaAccountListingItem.TotalBalance(
                    totalBalance = totalBalance,
                    currency = selectedCurrency,
                    metaAccount = metaAccount,
                    hasUpdates = hasUpdates
                )
            }
        }
    }

    private suspend fun getValidMetaAccountsForTransaction(metaAccountFilter: Filter<MetaAccount>): List<MetaAccount> {
        return accountRepository.getActiveMetaAccounts()
            .applyFilter(metaAccountFilter)
            .filter {
                when (it.type) {
                    LightMetaAccount.Type.SECRETS,
                    LightMetaAccount.Type.POLKADOT_VAULT,
                    LightMetaAccount.Type.PARITY_SIGNER,
                    LightMetaAccount.Type.PROXIED,
                    LightMetaAccount.Type.MULTISIG,
                    LightMetaAccount.Type.LEDGER,
                    LightMetaAccount.Type.LEDGER_LEGACY -> true

                    LightMetaAccount.Type.WATCH_ONLY -> false
                }
            }
    }

    private fun metaAccountStateComparator() = compareBy<LightMetaAccount.Status> {
        when (it) {
            LightMetaAccount.Status.ACTIVE -> 0
            LightMetaAccount.Status.DEACTIVATED -> 1
        }
    }
}
