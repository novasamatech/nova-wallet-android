package io.novafoundation.nova.feature_account_impl.data.sync

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.utils.filterToSet
import io.novafoundation.nova.common.utils.flatMapAsync
import io.novafoundation.nova.common.utils.mapToSet
import io.novafoundation.nova.common.utils.mutableMultiListMapOf
import io.novafoundation.nova.common.utils.put
import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus
import io.novafoundation.nova.feature_account_api.data.events.buildChangesEvent
import io.novafoundation.nova.feature_account_api.data.events.combineBusEvents
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.AddAccountResult
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.toEvent
import io.novafoundation.nova.feature_account_api.domain.account.identity.Identity
import io.novafoundation.nova.feature_account_api.domain.account.identity.IdentityProvider
import io.novafoundation.nova.feature_account_api.domain.account.identity.OnChainIdentity
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdKeyIn
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain


class ExternalAccountsSyncService(
    private val dataSources: List<ExternalAccountsSyncDataSource>,
    private val accountRepository: AccountRepository,
    private val accountDao: MetaAccountDao,
    private val eventBus: MetaAccountChangesEventBus,
    @OnChainIdentity private val identityProvider: IdentityProvider,
) {

    private val dataSource = CompoundExternalAccountsSyncDataSource(dataSources)

    suspend fun sync(chain: Chain) {
        val allAccounts = accountRepository.getAllMetaAccounts().filter { it.hasAccountIn(chain) }
        val directlyControlledAccounts = allAccounts.filter { !dataSource.isCreatedFromDataSource(it) }

        var nextSearchCandidates = directlyControlledAccounts.mapToSet { it.requireAccountIdKeyIn(chain) }
        val foundExternalAccounts = mutableListOf<ExternalControllableAccount>()
        val allVisitedCandidates = mutableSetOf<AccountIdKey>()

        while (nextSearchCandidates.isNotEmpty()) {
            val searchResults = dataSource.getControllableExternalAccounts(nextSearchCandidates, chain)
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

        val identities = identityProvider.identitiesFor(allVisitedCandidates.map { it.value }, chain.id)

        val existingAccountsByAccountId = allAccounts.groupBy { it.requireAccountIdKeyIn(chain) }
        val existingAccountsByParentId = allAccounts.groupBy { it.parentMetaId }

        val controllersByAccountId = mutableMultiListMapOf<AccountIdKey, Long>()
        existingAccountsByAccountId.onEach { (accountId, metaAccounts) ->
            val ids = metaAccounts.map { it.id }
            controllersByAccountId.put(accountId, ids)
        }

        val reachableExistingMetaIds = mutableSetOf<Long>()
        val added = mutableListOf<MetaAccountChangesEventBus.Event>()
        directlyControlledAccounts.forEach { reachableExistingMetaIds.add(it.id) }

        var position = accountDao.nextAccountPosition()

        foundExternalAccounts.onEach { externalAccount ->
            val controllers = controllersByAccountId[externalAccount.controllerAccountId].orEmpty()

            controllers.onEach { controller ->
                val existingControlledAccounts = existingAccountsByParentId[controller].orEmpty()

                val existingAccountRepresentedByExternal = existingControlledAccounts.find { existingAccount ->
                    val existingAccountId = existingAccount.requireAccountIdKeyIn(chain)
                    existingAccountId == externalAccount.accountId && externalAccount.isRepresentedBy(existingAccount)
                }

                if (existingAccountRepresentedByExternal != null) {
                    reachableExistingMetaIds.add(existingAccountRepresentedByExternal.id)
                } else {
                    val addResult = externalAccount.addAccount(controller, identities[externalAccount.accountId], position)

                    position++
                    controllersByAccountId.put(externalAccount.accountId, addResult.metaId)
                    added.add(addResult.toEvent())
                }
            }
        }

        val allMetaIds = allAccounts.mapToSet { it.id }
        val nonReachableMetaIds = allMetaIds - reachableExistingMetaIds

        accountDao.changeAccountsStatus(nonReachableMetaIds.toList(), MetaAccountLocal.Status.DEACTIVATED)
        accountDao.changeAccountsStatus(reachableExistingMetaIds.toList(), MetaAccountLocal.Status.ACTIVE)

        added.combineBusEvents()?.let { eventBus.notify(it, source = null) }
    }
}



interface ExternalControllableAccount {

    val accountId: AccountIdKey

    val controllerAccountId: AccountIdKey

    /**
     * Check whether [localAccount] represents self in the data-base
     * Implementation can assume that [accountId] and [controller] check has already been done
     */
    fun isRepresentedBy(localAccount: MetaAccount): Boolean

    /**
     * Add account to the data-base, WITHOUT notifying any external entities,
     * like [MetaAccountChangesEventBus] - this is expected to be done by the calling code
     *
     * @return id of newly created account
     */
    suspend fun addAccount(
        controllerMetaId: Long,
        identity: Identity?,
        position: Int
    ): AddAccountResult.AccountAdded
}

class CompoundExternalAccountsSyncDataSource(
    private val delegates: List<ExternalAccountsSyncDataSource>
) : ExternalAccountsSyncDataSource {

    override suspend fun isCreatedFromDataSource(metaAccount: MetaAccount): Boolean {
        return delegates.any { it.isCreatedFromDataSource(metaAccount) }
    }

    override suspend fun getControllableExternalAccounts(
        accountIdsToQuery: Set<AccountIdKey>,
        chain: Chain
    ): List<ExternalControllableAccount> {
        return delegates.flatMapAsync { it.getControllableExternalAccounts(accountIdsToQuery, chain) }
    }
}
