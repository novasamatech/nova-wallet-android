package io.novafoundation.nova.feature_account_impl.domain

import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.common.utils.amountFromPlanks
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.utils.removed
import io.novafoundation.nova.common.utils.sumByBigDecimal
import io.novafoundation.nova.feature_account_api.data.proxy.MetaAccountsUpdatesRegistry
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.MetaAccountGroupingInteractor
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccountAssetBalance
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccountWithTotalBalance
import io.novafoundation.nova.feature_account_api.domain.model.ProxiedAndProxyMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.addressIn
import io.novafoundation.nova.feature_account_api.domain.model.hasAccountIn
import io.novafoundation.nova.feature_currency_api.domain.interfaces.CurrencyRepository
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine

class MetaAccountGroupingInteractorImpl(
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val currencyRepository: CurrencyRepository,
    private val metaAccountsUpdatesRegistry: MetaAccountsUpdatesRegistry,
) : MetaAccountGroupingInteractor {

    override fun metaAccountsWithTotalBalanceFlow(): Flow<GroupedList<LightMetaAccount.Type, MetaAccountWithTotalBalance>> {
        return combine(
            currencyRepository.observeSelectCurrency(),
            accountRepository.activeMetaAccountsFlow(),
            accountRepository.metaAccountBalancesFlow(),
            metaAccountsUpdatesRegistry.observeUpdates(),
            chainRegistry.chainsById
        ) { selectedCurrency, accounts, allBalances, updatedMetaAccounts, chains ->
            val groupedBalances = allBalances.groupBy(MetaAccountAssetBalance::metaId)

            accounts.map { metaAccount ->
                val accountBalances = groupedBalances[metaAccount.id] ?: emptyList()
                val hasUpdates = updatedMetaAccounts.contains(metaAccount.id)
                metaAccountWithTotalBalance(accountBalances, metaAccount, accounts, selectedCurrency, chains, hasUpdates)
            }
                .groupBy { it.metaAccount.type }
                .toSortedMap(metaAccountTypeComparator())
        }
    }

    override fun metaAccountWithTotalBalanceFlow(metaId: Long): Flow<MetaAccountWithTotalBalance> {
        return combine(
            currencyRepository.observeSelectCurrency(),
            accountRepository.allMetaAccountsFlow(),
            accountRepository.metaAccountFlow(metaId),
            accountRepository.metaAccountBalancesFlow(metaId),
            chainRegistry.chainsById
        ) { selectedCurrency, allMetaAccounts, metaAccount, metaAccountBalances, chains ->
            metaAccountWithTotalBalance(metaAccountBalances, metaAccount, allMetaAccounts, selectedCurrency, chains, false)
        }
    }

    override fun getMetaAccountsForTransaction(fromId: ChainId, destinationId: ChainId): Flow<GroupedList<LightMetaAccount.Type, MetaAccount>> = flowOf {
        val fromChain = chainRegistry.getChain(fromId)
        val destinationChain = chainRegistry.getChain(destinationId)
        getValidMetaAccountsForTransaction(fromChain, destinationChain)
            .groupBy(MetaAccount::type)
            .toSortedMap(metaAccountTypeComparator())
    }

    override fun updatedProxieds(): Flow<GroupedList<LightMetaAccount.State, ProxiedAndProxyMetaAccount>> {
        return combine(
            metaAccountsUpdatesRegistry.observeUpdates(),
            accountRepository.allMetaAccountsFlow(),
            chainRegistry.chainsById
        ) { metaIds, metaAccount, chainsById ->
            val metaById = metaAccount.associateBy(MetaAccount::id)
            metaAccount
                .filter { it.type == LightMetaAccount.Type.PROXIED && metaIds.contains(it.id) }
                .map {
                    ProxiedAndProxyMetaAccount(
                        it,
                        metaById[it.proxy?.metaId] ?: error("Proxy meta account not found"),
                        chainsById[it.proxy?.chainId] ?: error("Proxy chain not found")
                    )
                }
                .groupBy { it.proxied.state }
        }.catch { emit(emptyMap()) }
    }

    override suspend fun hasAvailableMetaAccountsForDestination(fromId: ChainId, destinationId: ChainId): Boolean {
        val fromChain = chainRegistry.getChain(fromId)
        val destinationChain = chainRegistry.getChain(destinationId)
        return getValidMetaAccountsForTransaction(fromChain, destinationChain)
            .any { it.hasAccountIn(destinationChain) }
    }

    private suspend fun metaAccountWithTotalBalance(
        metaAccountBalances: List<MetaAccountAssetBalance>,
        metaAccount: MetaAccount,
        allMetaAccounts: List<MetaAccount>,
        selectedCurrency: Currency,
        chains: Map<ChainId, Chain>,
        hasUpdates: Boolean
    ): MetaAccountWithTotalBalance {
        val totalBalance = metaAccountBalances.sumByBigDecimal {
            val totalInPlanks = it.freeInPlanks + it.reservedInPlanks + it.offChainBalance.orZero()

            totalInPlanks.amountFromPlanks(it.precision) * it.rate.orZero()
        }

        val proxyMetaAccount = metaAccount.proxy?.let { proxy -> allMetaAccounts.firstOrNull { it.id == proxy.metaId } }

        return MetaAccountWithTotalBalance(
            metaAccount = metaAccount,
            proxyMetaAccount = proxyMetaAccount,
            proxyChain = metaAccount.proxy?.chainId?.let(chains::getValue),
            totalBalance = totalBalance,
            currency = selectedCurrency,
            hasUpdates = hasUpdates
        )
    }

    private suspend fun getValidMetaAccountsForTransaction(from: Chain, destination: Chain): List<MetaAccount> {
        val selectedMetaAccount = accountRepository.getSelectedMetaAccount()
        val fromChainAddress = selectedMetaAccount.addressIn(from)
        return accountRepository.allMetaAccounts()
            .removed { fromChainAddress == it.addressIn(destination) }
            .filter {
                when (it.type) {
                    LightMetaAccount.Type.SECRETS,
                    LightMetaAccount.Type.POLKADOT_VAULT,
                    LightMetaAccount.Type.PARITY_SIGNER,
                    LightMetaAccount.Type.PROXIED,
                    LightMetaAccount.Type.LEDGER -> true

                    LightMetaAccount.Type.WATCH_ONLY -> false
                }
            }
    }

    private fun metaAccountTypeComparator() = compareBy<LightMetaAccount.Type> {
        when (it) {
            LightMetaAccount.Type.SECRETS -> 0
            LightMetaAccount.Type.POLKADOT_VAULT -> 1
            LightMetaAccount.Type.PARITY_SIGNER -> 2
            LightMetaAccount.Type.LEDGER -> 3
            LightMetaAccount.Type.PROXIED -> 4
            LightMetaAccount.Type.WATCH_ONLY -> 5
        }
    }
}
