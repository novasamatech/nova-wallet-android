package io.novafoundation.nova.feature_account_impl.data.sync

import android.util.Log
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.common.utils.filterToSet
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.mapToSet
import io.novafoundation.nova.common.utils.mutableMultiListMapOf
import io.novafoundation.nova.common.utils.put
import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus
import io.novafoundation.nova.feature_account_api.data.events.combineBusEvents
import io.novafoundation.nova.feature_account_api.data.externalAccounts.ExternalAccountsSyncService
import io.novafoundation.nova.feature_account_api.data.proxy.MetaAccountsUpdatesRegistry
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.AddAccountResult
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.toAccountBusEvent
import io.novafoundation.nova.feature_account_api.domain.account.identity.Identity
import io.novafoundation.nova.feature_account_api.domain.account.identity.IdentityProvider
import io.novafoundation.nova.feature_account_api.domain.account.identity.OnChainIdentity
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdKeyIn
import io.novafoundation.nova.feature_account_impl.BuildConfig
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.enabledChains
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@FeatureScope
internal class RealExternalAccountsSyncService @Inject constructor(
    private val dataSourceFactories: Set<@JvmSuppressWildcards ExternalAccountsSyncDataSource.Factory>,
    private val accountRepository: AccountRepository,
    private val accountDao: MetaAccountDao,
    private val eventBus: MetaAccountChangesEventBus,
    private val metaAccountsUpdatesRegistry: MetaAccountsUpdatesRegistry,
    @OnChainIdentity private val identityProvider: IdentityProvider,
    private val rootScope: RootScope,
    private val chainRegistry: ChainRegistry,
) : ExternalAccountsSyncService {

    private val canSyncWatchOnly = BuildConfig.DEBUG
    private val syncMutex = Mutex()

    companion object {

        private const val ACCOUNTS_CHANGED_SOURCE = "ExternalAccountsSyncService"
    }


    override fun syncOnAccountChange(changeSource: String?) {
        if (changeSource != ACCOUNTS_CHANGED_SOURCE) {
            sync()
        }
    }

    override fun sync() = rootScope.launchUnit(Dispatchers.IO) {
        syncMutex.withLock {
            chainRegistry.enabledChains().forEach { chain ->
                syncInternal(chain)
            }
        }
    }

    override fun sync(chain: Chain) = rootScope.launchUnit(Dispatchers.IO) {
        syncMutex.withLock {
            syncInternal(chain)
        }
    }

    private suspend fun syncInternal(chain: Chain) {
        val dataSources = dataSourceFactories.mapNotNull { it.create(chain) }
        if (dataSources.isNotEmpty()) {
            val aggregateSource = dataSources.aggregate()
            sync(chain, aggregateSource)
        }
    }

    private suspend fun sync(chain: Chain, dataSource: ExternalAccountsSyncDataSource) = runCatching {
        Log.d("ExternalAccountsDiscovery", "Started syncing external accounts on ${chain.name}")

        val allAccounts = accountRepository.getAllMetaAccounts().filter {
            it.isAllowedToSyncExternalAccounts() && it.hasAccountIn(chain)
        }
        val directlyControlledAccounts = allAccounts.filter { !dataSource.isCreatedFromDataSource(it) }
        if (directlyControlledAccounts.isEmpty()) return@runCatching

        val (externalAccounts, allVisitedCandidates) = findReachableExternalAccounts(directlyControlledAccounts, dataSource, chain)

        val identities = identityProvider.identitiesFor(allVisitedCandidates.map { it.value }, chain.id)

        val (added, reachableExistingMetaIds) = addNewExternalAccounts(allAccounts, directlyControlledAccounts, externalAccounts, identities, dataSource, chain)

        updateAccountStatuses(allAccounts, reachableExistingMetaIds)
        notifyAboutAddedAccounts(added)
    }
        .onFailure { Log.d("ExternalAccountsDiscovery", "Failed to sync external accounts for chain ${chain.name}", it) }
        .onSuccess { Log.d("ExternalAccountsDiscovery", "Finished syncing external accounts for chain ${chain.name}") }

    private suspend fun notifyAboutAddedAccounts(added: List<AddAccountResult.AccountAdded>) {
        added.map { it.toAccountBusEvent() }
            .combineBusEvents()
            ?.let { eventBus.notify(it, source = ACCOUNTS_CHANGED_SOURCE) }

        metaAccountsUpdatesRegistry.addMetaIds(added.map { it.metaId })
    }

    private suspend fun updateAccountStatuses(
        allAccounts: List<MetaAccount>,
        stillReachableExistingIds: Set<Long>
    ) {
        val allMetaIds = allAccounts.mapToSet { it.id }
        val nonReachableMetaIds = allMetaIds - stillReachableExistingIds

        accountDao.changeAccountsStatus(nonReachableMetaIds.toList(), MetaAccountLocal.Status.DEACTIVATED)
        accountDao.changeAccountsStatus(stillReachableExistingIds.toList(), MetaAccountLocal.Status.ACTIVE)
    }

    private suspend fun addNewExternalAccounts(
        allAccounts: List<MetaAccount>,
        directlyControlledAccounts: List<MetaAccount>,
        foundExternalAccounts: List<ExternalControllableAccount>,
        identities: Map<AccountIdKey, Identity?>,
        dataSource: ExternalAccountsSyncDataSource,
        chain: Chain
    ): AddReachableAccountResult {
        val existingAccountsByAccountId = allAccounts.groupBy { it.requireAccountIdKeyIn(chain) }
        val existingAccountsByParentId = allAccounts.groupBy { it.parentMetaId }

        val controllersByAccountId = mutableMultiListMapOf<AccountIdKey, MetaAccount>()
        existingAccountsByAccountId.onEach { (accountId, metaAccounts) ->
            controllersByAccountId.put(accountId, metaAccounts)
        }

        val reachableExistingMetaIds = mutableSetOf<Long>()
        val added = mutableListOf<AddAccountResult.AccountAdded>()
        directlyControlledAccounts.forEach { reachableExistingMetaIds.add(it.id) }

        var position = accountDao.nextAccountPosition()

        foundExternalAccounts.onEach { externalAccount ->
            val controllers = controllersByAccountId[externalAccount.controllerAccountId].orEmpty()

            controllers.onEach { controller ->
                val existingControlledAccounts = existingAccountsByParentId[controller.id].orEmpty()

                val existingAccountRepresentedByExternal = existingControlledAccounts.find { existingAccount ->
                    val existingAccountId = existingAccount.requireAccountIdKeyIn(chain)
                    existingAccountId == externalAccount.accountId && externalAccount.isRepresentedBy(existingAccount)
                }

                if (existingAccountRepresentedByExternal != null) {
                    reachableExistingMetaIds.add(existingAccountRepresentedByExternal.id)
                } else {
                    val controllerAsExternal = dataSource.getExternalCreatedAccount(controller)
                    val canControl = controllerAsExternal == null || controllerAsExternal.canControl(externalAccount)

                    if (canControl) {
                        val addResult = externalAccount.addAccount(controller, identities[externalAccount.accountId], position)

                        val newMetaAccount = accountRepository.getMetaAccount(addResult.metaId)

                        position++
                        controllersByAccountId.put(externalAccount.accountId, newMetaAccount)
                        added.add(addResult)
                    } else {
                        val controlledAddress = chain.addressOf(externalAccount.accountId)
                        val controllerAddress = chain.addressOf(externalAccount.controllerAccountId)
                        Log.v("ExternalAccountsDiscovery", "Discovered account $controlledAddress cannot be controlled by $controllerAddress")
                    }
                }
            }
        }

        Log.d("ExternalAccountsDiscovery", "Added ${added.size} new accounts on ${chain.name}")

        return AddReachableAccountResult(added, reachableExistingMetaIds)
    }

    private suspend fun findReachableExternalAccounts(
        directlyControlledAccounts: List<MetaAccount>,
        dataSource: ExternalAccountsSyncDataSource,
        chain: Chain,
    ): ReachableExternalAccounts {
        var nextSearchCandidates = directlyControlledAccounts.mapToSet { it.requireAccountIdKeyIn(chain) }
        val foundExternalAccounts = mutableListOf<ExternalControllableAccount>()
        val allVisitedCandidates = mutableSetOf<AccountIdKey>()

        while (nextSearchCandidates.isNotEmpty()) {
            val searchResults = dataSource.getControllableExternalAccounts(nextSearchCandidates)
            foundExternalAccounts.addAll(searchResults)

            val previousSearchCandidates = nextSearchCandidates

            // We do not search already visited account ids as it might result in endless recursion
            // Note however, that we still add such results to the found accounts since connection TO the visited account
            // might be useful (e.g. we discovered alternative path to already visited account)
            nextSearchCandidates = searchResults
                .map { it.accountId }
                .filterToSet { it !in allVisitedCandidates }

            allVisitedCandidates.addAll(previousSearchCandidates)
        }

        return ReachableExternalAccounts(foundExternalAccounts, allVisitedCandidates)
    }

    private data class ReachableExternalAccounts(
        val accounts: List<ExternalControllableAccount>,
        val allVisitedCandidates: Set<AccountIdKey>
    )

    private data class AddReachableAccountResult(
        val added: List<AddAccountResult.AccountAdded>,
        val stillReachableExistingIds: Set<Long>
    )

    private fun List<ExternalAccountsSyncDataSource>.aggregate(): ExternalAccountsSyncDataSource {
        return when (size) {
            0 -> error("Empty data-sources list")
            1 -> single()
            else -> CompoundExternalAccountsSyncDataSource(this)
        }
    }

    private fun MetaAccount.isAllowedToSyncExternalAccounts(): Boolean {
        return if (type == LightMetaAccount.Type.WATCH_ONLY) {
            canSyncWatchOnly
        } else {
            true
        }
    }
}
