package io.novafoundation.nova.feature_account_impl.data.sync

import android.util.Log
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.associateMutableBy
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.common.utils.filterToSet
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.mapToSet
import io.novafoundation.nova.common.utils.put
import io.novafoundation.nova.common.utils.toMutableMultiMapList
import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus
import io.novafoundation.nova.feature_account_api.data.events.checkIncludes
import io.novafoundation.nova.feature_account_api.data.events.combineBusEvents
import io.novafoundation.nova.feature_account_api.data.externalAccounts.ExternalAccountsSyncService
import io.novafoundation.nova.feature_account_api.data.model.AccountIdKeyMap
import io.novafoundation.nova.feature_account_api.data.model.OnChainIdentity
import io.novafoundation.nova.feature_account_api.data.proxy.MetaAccountsUpdatesRegistry
import io.novafoundation.nova.feature_account_api.data.repository.OnChainIdentityRepository
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.AddAccountResult
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.toAccountBusEvent
import io.novafoundation.nova.feature_account_api.domain.account.identity.Identity
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.accountIdKeyIn
import io.novafoundation.nova.feature_account_api.domain.model.isUniversal
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdKeyIn
import io.novafoundation.nova.feature_account_impl.BuildConfig
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
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
    private val identityRepository: OnChainIdentityRepository,
    private val rootScope: RootScope,
    private val chainRegistry: ChainRegistry,
) : ExternalAccountsSyncService {

    private val canSyncWatchOnly = BuildConfig.DEBUG
    private val syncMutex = Mutex()

    companion object {

        private const val ACCOUNTS_CHANGED_SOURCE = "ExternalAccountsSyncService"
    }

    override fun syncOnAccountChange(event: MetaAccountChangesEventBus.Event, changeSource: String?) {
        val hasRelevantEvents = event.checkIncludes(
            checkAdd = true,
            checkStructureChange = true,
            checkNameChange = false, // Name changes are irrelevant as they don't affect structure
            checkAccountRemoved = false // All dependent accounts are cleaned up automatically by DB so we don't need to re-sync on delete
        )
        val notCausedByItself = changeSource != ACCOUNTS_CHANGED_SOURCE

        if (hasRelevantEvents && notCausedByItself) {
            sync()
        } else {
            Log.d(
                "ExternalAccountsDiscovery",
                "syncOnAccountChange was ignored due to conditions not being met:" +
                    " hasRelevantEvents=$hasRelevantEvents, notCausedByItself=$notCausedByItself"
            )
        }
    }

    override fun sync() = rootScope.launchUnit(Dispatchers.IO) {
        syncMutex.withLock {
            syncInternal()
        }
    }

    private suspend fun syncInternal() {
        val dataSource = dataSourceFactories
            .map { it.create() }
            .aggregate()

        sync(dataSource)
    }

    /**
     * Sync all reachable accounts from accounts that user directly controls.
     *
     * The terminology used here:
     * * Controller - an account that can operation on behalf of another account
     * * Controlled accounts - account that is controlled by a controller
     *
     * The high-level of the algorithm is the following:
     * 1. We perform BFS starting from accounts user directly controls, fetching reachable accounts iteratively:
     * once we fetched a new non-empty list of controllable accounts from data-sources, we start fetching again, now with just obtained accounts
     * This way, we will be able to crawl all the reachable accounts
     * Note that at this step, we do not yet check whether a certain pair of Controller + Controlled is actually usable
     * Also, this step operates just with account ids and not with wallet ids to make it easier to implement data-sources
     * It is also important to note, that each [ExternalControllableAccount] represents a unique Controller + Controlled pair. However, if there are
     * several meta accounts with the same account ids that are controllers, [ExternalControllableAccount] will result in a separate meta account created for each of them
     * On the other hand, if multiple different controllers control a single controlled accounts, they will have different corresponding [ExternalControllableAccount] instances
     *
     * 2. Once we fetched all accessible [ExternalControllableAccount] we start comparing already added meta accounts with the fetched list. The goal is to only add those accounts
     * that has not been added yet
     * For each external account, [updateLocalExternalAccounts] achieve this by doing the following:
     *   a. It first checks whether this pair of controller + controlled can actually be used.
     *   In particular, a controller, via [ExternalSourceCreatedAccount], checks that it can actually control the controlled account. This is usefully for Proxies
     *   since only Any/NonTransfer proxy account can dispatch proxy.proxy / multisig.as_multi calls, and thus, be able to control such accounts despite the permission being granted
     *   b. Get meta accounts for controllers that can control this external account
     *   c. For each controller, check all of its controlled accounts that are already in db. This is done via [MetaAccount.parentMetaId] field
     *   d. Compare each such controlled account with external account we are analyzing.
     *   Check is done comparing accountId of controlled account + allowing [ExternalControllableAccount] to do some extra check
     *   For example, it is not enough for a Proxy to just check for controlled and controller account ids match
     *   as there might be multiple connection between same pairs of accounts via multiple proxy types.
     */
    private suspend fun sync(dataSource: ExternalAccountsSyncDataSource): Result<Unit> = runCatching {
        Log.d("ExternalAccountsDiscovery", "Started syncing external accounts")

        val directlyControlledAccounts = accountRepository.getAllMetaAccounts()
            .filter { it.isAllowedToSyncExternalAccounts() && !dataSource.isCreatedFromDataSource(it) }
        if (directlyControlledAccounts.isEmpty()) return@runCatching

        val supportedChains = dataSource.supportedChains()

        val (externalAccounts, allVisitedCandidates) = findReachableExternalAccounts(directlyControlledAccounts, dataSource, supportedChains)

        val identities = identityRepository.getMultiChainIdentities(allVisitedCandidates)

        updateLocalExternalAccounts(directlyControlledAccounts, externalAccounts, identities, dataSource, supportedChains)
    }
        .onFailure { Log.d("ExternalAccountsDiscovery", "Failed to sync external accounts", it) }
        .onSuccess { Log.d("ExternalAccountsDiscovery", "Finished syncing external accounts ") }

    private suspend fun notifyAboutAddedAccounts(added: List<AddAccountResult.AccountAdded>, deactivatedMetaIds: Set<Long>) {
        added.map { it.toAccountBusEvent() }
            .combineBusEvents()
            ?.let { eventBus.notify(it, source = ACCOUNTS_CHANGED_SOURCE) }

        val updatedAccountsIds = deactivatedMetaIds.toList() + added.map { it.metaId }
        metaAccountsUpdatesRegistry.addMetaIds(updatedAccountsIds)
    }

    private fun constructReachabilityReport(
        allAccounts: List<MetaAccount>,
        stillReachableExistingIds: Set<Long>,
    ): ChainReachabilityReport {
        val (universalAccounts, singleChainAccounts) = allAccounts.partition { it.isUniversal() }
        val universalAccountIds = universalAccounts.mapToSet { it.id }
        val singleChainAccountIds = singleChainAccounts.mapToSet { it.id }

        return ChainReachabilityReport(
            singleChainNonReachable = singleChainAccountIds - stillReachableExistingIds,
            universalNonReachable = universalAccountIds - stillReachableExistingIds,
            stillReachable = stillReachableExistingIds
        )
    }

    private suspend fun updateAccountStatusesForSingleChain(reachabilityReport: ChainReachabilityReport, chain: Chain) {
        val singleChainNonReachable = reachabilityReport.singleChainNonReachable
        if (singleChainNonReachable.isNotEmpty()) {
            Log.d(
                "ExternalAccountsDiscovery",
                "Disabling ${singleChainNonReachable.size} non-reachable accounts" +
                    " when syncing ${chain.name}: $singleChainNonReachable"
            )
            accountDao.changeAccountsStatus(singleChainNonReachable.toList(), MetaAccountLocal.Status.DEACTIVATED)
        } else {
            Log.d("ExternalAccountsDiscovery", "No accounts to disable found when syncing ${chain.name}")
        }

        val reachable = reachabilityReport.stillReachable
        Log.d("ExternalAccountsDiscovery", "Enabling ${reachable.size} still-reachable accounts when syncing ${chain.name}: $reachable")
        accountDao.changeAccountsStatus(reachable.toList(), MetaAccountLocal.Status.ACTIVE)
    }

    private suspend fun updateAccountStatusForUniversal(notReachableUniversalPerChain: List<NonReachableUniversalIds>) {
        if (notReachableUniversalPerChain.isEmpty()) return

        // Find universal account that every chain reported as non-reachable
        val notReachable = notReachableUniversalPerChain.reduce { a, b -> a.intersect(b) }
            .toList()

        if (notReachable.isNotEmpty()) {
            Log.d("ExternalAccountsDiscovery", "Disabling ${notReachable.size} non-reachable universal accounts: $notReachable")
            accountDao.changeAccountsStatus(notReachable, MetaAccountLocal.Status.DEACTIVATED)
            metaAccountsUpdatesRegistry.addMetaIds(notReachable)
        } else {
            Log.d("ExternalAccountsDiscovery", "No universal accounts to disable found")
        }
    }

    private suspend fun updateLocalExternalAccounts(
        directlyControlledAccounts: List<MetaAccount>,
        externalAccounts: List<ExternalControllableAccount>,
        identities: AccountIdKeyMap<OnChainIdentity?>,
        dataSource: ExternalAccountsSyncDataSource,
        supportedChains: Collection<Chain>,
    ) {
        val universalNonReachable = supportedChains.mapNotNull { chain ->
            runCatching {
                val allAccounts = accountRepository.getAllMetaAccounts().filter { it.isAllowedToSyncExternalAccounts() }

                val allAccountsAvailableOnChain = allAccounts.filter { it.hasAccountIn(chain) }
                val directAccountsAvailableOnChain = directlyControlledAccounts.filter { it.hasAccountIn(chain) }

                val externalAccountsAvailableOnChain = externalAccounts.filter { it.isAvailableOn(chain) }

                val (added, reachableExistingMetaIds) = addNewExternalAccounts(
                    allAccounts = allAccountsAvailableOnChain,
                    directlyControlledAccounts = directAccountsAvailableOnChain,
                    foundExternalAccounts = externalAccountsAvailableOnChain,
                    identities = identities,
                    dataSource = dataSource,
                    chain = chain
                )

                val reachabilityReport = constructReachabilityReport(allAccountsAvailableOnChain, reachableExistingMetaIds)

                updateAccountStatusesForSingleChain(reachabilityReport, chain)
                notifyAboutAddedAccounts(added, deactivatedMetaIds = reachabilityReport.singleChainNonReachable)

                reachabilityReport.universalNonReachable
            }
                .onFailure { Log.d("ExternalAccountsDiscovery", "Failed to add new external accounts for chain ${chain.name}", it) }
                .onSuccess { Log.d("ExternalAccountsDiscovery", "Finished adding new external accounts for chain ${chain.name}") }
                .getOrNull()
        }

        updateAccountStatusForUniversal(universalNonReachable)
    }

    private suspend fun addNewExternalAccounts(
        allAccounts: List<MetaAccount>,
        directlyControlledAccounts: List<MetaAccount>,
        foundExternalAccounts: List<ExternalControllableAccount>,
        identities: AccountIdKeyMap<OnChainIdentity?>,
        dataSource: ExternalAccountsSyncDataSource,
        chain: Chain
    ): AddReachableAccountResult {
        val existingAccountsByAccountId = allAccounts.groupBy { it.requireAccountIdKeyIn(chain) }
        val existingAccountsByParentId = allAccounts.groupBy { it.parentMetaId }

        val controllersByParentId = existingAccountsByParentId.toMutableMultiMapList()
        val controllersByAccountId = existingAccountsByAccountId.toMutableMultiMapList()
        val controllersById = allAccounts.associateMutableBy { it.id }

        fun signingPath(metaAccount: MetaAccount): List<AccountIdKey> {
            val path = mutableListOf(metaAccount.requireAccountIdKeyIn(chain))
            var current = metaAccount

            while (current.parentMetaId != null) {
                val parent = controllersById[current.parentMetaId] ?: break
                path.add(parent.requireAccountIdKeyIn(chain))
                current = parent
            }

            return path
        }

        val reachableExistingMetaIds = mutableSetOf<Long>()
        val added = mutableListOf<AddAccountResult.AccountAdded>()
        directlyControlledAccounts.forEach { reachableExistingMetaIds.add(it.id) }

        var position = accountDao.nextAccountPosition()

        Log.d("ExternalAccountsDiscovery", "Checking ${foundExternalAccounts.size} found external accounts against local state in ${chain.name}")

        foundExternalAccounts.onEach { externalAccount ->
            val controllers = controllersByAccountId[externalAccount.controllerAccountId].orEmpty()

            controllers.onEach controllersLoop@{ controller ->
                val signingPath = signingPath(controller)
                if (externalAccount.accountId in signingPath) {
                    Log.v(
                        "ExternalAccountsDiscovery",
                        "Loop detected: ${externalAccount.address(chain)} already present " +
                            "in the signing path of ${externalAccount.controllerAddress(chain)}"
                    )
                    return@controllersLoop
                }

                val controllerAsExternal = dataSource.getExternalCreatedAccount(controller)
                val canControl = controllerAsExternal == null || controllerAsExternal.canControl(externalAccount)

                if (!canControl) {
                    Log.v(
                        "ExternalAccountsDiscovery",
                        "Discovered account ${externalAccount.address(chain)} cannot be controlled by ${externalAccount.controllerAddress(chain)}"
                    )
                    return@controllersLoop
                }

                val existingControlledAccounts = controllersByParentId[controller.id].orEmpty()

                val existingAccountRepresentedByExternal = existingControlledAccounts.find { existingAccount ->
                    val existingAccountId = existingAccount.requireAccountIdKeyIn(chain)
                    existingAccountId == externalAccount.accountId && externalAccount.isRepresentedBy(existingAccount)
                }

                if (existingAccountRepresentedByExternal != null) {
                    reachableExistingMetaIds.add(existingAccountRepresentedByExternal.id)
                } else {
                    val identity = identities[externalAccount.accountId]?.let(::Identity)
                    val addResult = externalAccount.addControlledAccount(controller, identity, position, chain)

                    val newMetaAccount = accountRepository.getMetaAccount(addResult.metaId)

                    position++
                    controllersByAccountId.put(externalAccount.accountId, newMetaAccount)
                    controllersByParentId.put(controller.id, newMetaAccount)

                    controllersById[newMetaAccount.id] = newMetaAccount
                    added.add(addResult)
                }
            }
        }

        Log.d("ExternalAccountsDiscovery", "Added ${added.size} new accounts on ${chain.name}: ${added.map { it.type }}")

        return AddReachableAccountResult(added, reachableExistingMetaIds)
    }

    private suspend fun findReachableExternalAccounts(
        directlyControlledAccounts: List<MetaAccount>,
        dataSource: ExternalAccountsSyncDataSource,
        supportedChains: Collection<Chain>
    ): ReachableExternalAccounts {
        var nextSearchCandidates = getAccountIds(directlyControlledAccounts, supportedChains)
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

    private fun getAccountIds(metaAccounts: List<MetaAccount>, chains: Collection<Chain>): Set<AccountIdKey> {
        return buildSet {
            metaAccounts.onEach {
                addAccountIds(it, chains)
            }
        }
    }

    context(MutableSet<AccountIdKey>)
    private fun addAccountIds(metaAccount: MetaAccount, chains: Collection<Chain>) {
        chains.forEach { chain ->
            metaAccount.accountIdKeyIn(chain)?.let { add(it) }
        }
    }

    private data class ChainReachabilityReport(
        val singleChainNonReachable: Set<Long>,
        val universalNonReachable: Set<Long>,
        val stillReachable: Set<Long>
    )

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

private typealias NonReachableUniversalIds = Set<Long>
