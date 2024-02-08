package io.novafoundation.nova.feature_account_impl.data.proxy

import android.util.Log
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.common.utils.mapToSet
import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import io.novafoundation.nova.core_db.model.chain.account.ProxyAccountLocal
import io.novafoundation.nova.feature_account_api.data.proxy.MetaAccountsUpdatesRegistry
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.proxied.ProxiedAddAccountRepository
import io.novafoundation.nova.feature_account_api.data.proxy.ProxySyncService
import io.novafoundation.nova.feature_account_api.domain.account.identity.Identity
import io.novafoundation.nova.feature_account_api.domain.account.identity.IdentityProvider
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.hasAccountIn
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_proxy_api.data.common.NestedProxiesGraphConstructor
import io.novafoundation.nova.feature_proxy_api.data.common.getAllAccountIds
import io.novafoundation.nova.feature_proxy_api.data.repository.GetProxyRepository
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.findChains
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private class CreateMetaAccountsResult(
    val addedMetaIds: MutableList<Long> = mutableListOf(),
    val alreadyExistedMetaIds: MutableList<Long> = mutableListOf(),
    val shouldBeActivatedMetaIds: MutableList<Long> = mutableListOf()
) {

    fun add(other: CreateMetaAccountsResult) {
        addedMetaIds.addAll(other.addedMetaIds)
        alreadyExistedMetaIds.addAll(other.alreadyExistedMetaIds)
        shouldBeActivatedMetaIds.addAll(other.shouldBeActivatedMetaIds)
    }
}

class RealProxySyncService(
    private val chainRegistry: ChainRegistry,
    private val getProxyRepository: GetProxyRepository,
    private val accountRepository: AccountRepository,
    private val accountDao: MetaAccountDao,
    private val identityProvider: IdentityProvider,
    private val metaAccountsUpdatesRegistry: MetaAccountsUpdatesRegistry,
    private val proxiedAddAccountRepository: ProxiedAddAccountRepository,
    private val rootScope: RootScope,
    private val shouldSyncWatchOnlyProxies: Boolean
) : ProxySyncService {

    override fun proxySyncTrigger(): Flow<*> {
        return chainRegistry.currentChains.map { chains ->
            chains
                .filter(Chain::supportProxy)
                .map(Chain::id)
        }.distinctUntilChanged()
    }

    override fun startSyncing() {
        rootScope.launch(Dispatchers.Default) {
            startSyncInternal()
        }
    }

    override suspend fun startSyncingSuspend() {
        startSyncInternal()
    }

    private suspend fun startSyncInternal() = runCatching {
        val metaAccounts = getMetaAccounts()
        if (metaAccounts.isEmpty()) return@runCatching

        val supportedProxyChains = getSupportedProxyChains()

        Log.d(LOG_TAG, "Starting syncing proxies in ${supportedProxyChains.size} chains")

        supportedProxyChains.forEach { chain ->
            syncChainProxies(chain, metaAccounts)
        }
    }.onFailure {
        Log.e(LOG_TAG, "Failed to sync proxy delegators", it)
    }

    private suspend fun syncChainProxies(chain: Chain, allMetaAccounts: List<MetaAccount>) = runCatching {
        Log.d(LOG_TAG, "Started syncing proxies for ${chain.name}")

        val availableAccounts = chain.getAvailableMetaAccounts(allMetaAccounts)
        val availableAccountIds = availableAccounts.mapToSet { it.requireAccountIdIn(chain).intoKey() }

        val nodes = getProxyRepository.findAllProxiedsForAccounts(chain.id, availableAccountIds)
        val proxiedAccountIds = NestedProxiesGraphConstructor.Node.getAllAccountIds(nodes)

        val oldProxies = accountDao.getProxyAccounts(chain.id)
        val identities = identityProvider.identitiesFor(proxiedAccountIds, chain.id)

        val result = createMetaAccounts(chain, oldProxies, identities, availableAccounts, nodes)

        val deactivatedMetaIds = result.findDeactivated(oldProxies)
        accountDao.changeAccountsStatus(deactivatedMetaIds, MetaAccountLocal.Status.DEACTIVATED)
        accountDao.changeAccountsStatus(result.shouldBeActivatedMetaIds, MetaAccountLocal.Status.ACTIVE)

        val changedMetaIds = result.addedMetaIds + deactivatedMetaIds
        metaAccountsUpdatesRegistry.addMetaIds(changedMetaIds)
    }.onFailure {
        Log.e(LOG_TAG, "Failed to sync proxy delegators in chain ${chain.name}", it)
    }.onSuccess {
        Log.d(LOG_TAG, "Finished syncing proxies for ${chain.name}")
    }

    private suspend fun createMetaAccounts(
        chain: Chain,
        oldProxies: List<ProxyAccountLocal>,
        identities: Map<AccountIdKey, Identity?>,
        maybeProxyMetaAccounts: List<MetaAccount>,
        startNodes: List<NestedProxiesGraphConstructor.Node>
    ): CreateMetaAccountsResult {
        val result = CreateMetaAccountsResult()

        val accountIdToNode = startNodes.associateBy { it.accountId }
        for (metaAccount in maybeProxyMetaAccounts) {
            val proxyAccountId = metaAccount.requireAccountIdIn(chain).intoKey()
            val startNode = accountIdToNode[proxyAccountId] ?: continue // skip adding proxieds for metaAccount if it's not in the tree

            val nestedResult = recursivelyCreateMetaAccounts(chain.id, oldProxies, identities, metaAccount.id, startNode.nestedNodes)
            result.add(nestedResult)
        }

        return result
    }

    private suspend fun recursivelyCreateMetaAccounts(
        chainId: ChainId,
        oldProxies: List<ProxyAccountLocal>,
        identities: Map<AccountIdKey, Identity?>,
        proxyMetaId: Long,
        nestedNodes: List<NestedProxiesGraphConstructor.Node>
    ): CreateMetaAccountsResult {
        val result = CreateMetaAccountsResult()

        for (node in nestedNodes) {
            val maybeExistedProxiedMetaId = node.getExistedProxiedMetaId(chainId, oldProxies, proxyMetaId)

            var nextMetaId = if (maybeExistedProxiedMetaId == null) {
                val newMetaId = addProxiedAccount(chainId, node, proxyMetaId, identities)
                result.addedMetaIds.add(newMetaId)
                newMetaId
            } else {
                // An account may be deactivated but not deleted yet in case when we remove a proxy and then add it again
                // To support this case we should track deactivated accounts and activate them again
                val existedMetaAccount = accountRepository.getMetaAccount(maybeExistedProxiedMetaId)
                if (existedMetaAccount.status == LightMetaAccount.Status.DEACTIVATED) {
                    result.shouldBeActivatedMetaIds.add(maybeExistedProxiedMetaId)
                }

                result.alreadyExistedMetaIds.add(maybeExistedProxiedMetaId)
                maybeExistedProxiedMetaId
            }

            if (node.nestedNodes.isNotEmpty()) {
                val nestedResult = recursivelyCreateMetaAccounts(chainId, oldProxies, identities, nextMetaId, node.nestedNodes)
                result.add(nestedResult)
            }
        }

        return result
    }

    private suspend fun addProxiedAccount(
        chainId: ChainId,
        node: NestedProxiesGraphConstructor.Node,
        metaId: Long,
        identities: Map<AccountIdKey, Identity?>
    ) = proxiedAddAccountRepository.addAccount(
        ProxiedAddAccountRepository.Payload(
            chainId = chainId,
            proxiedAccountId = node.accountId.value,
            proxyType = node.permissionType,
            proxyMetaId = metaId,
            identity = identities[node.accountId]
        )
    )

    private fun NestedProxiesGraphConstructor.Node.getExistedProxiedMetaId(
        chainId: ChainId,
        oldProxies: List<ProxyAccountLocal>,
        proxyMetaId: Long
    ): Long? {
        val oldIdentifiers = oldProxies.associateBy { it.identifier }

        val identifier = ProxyAccountLocal.makeIdentifier(
            proxyMetaId = proxyMetaId,
            chainId = chainId,
            proxiedAccountId = accountId.value,
            proxyType = permissionType.name
        )

        return oldIdentifiers[identifier]?.proxiedMetaId
    }

    private suspend fun getMetaAccounts(): List<MetaAccount> {
        return accountRepository.getActiveMetaAccounts()
            .filter { it.isAllowedToSyncProxy(shouldSyncWatchOnlyProxies) }
    }

    private suspend fun getSupportedProxyChains(): List<Chain> {
        return chainRegistry.findChains { it.supportProxy }
    }

    private fun Chain.getAvailableMetaAccounts(metaAccounts: List<MetaAccount>): List<MetaAccount> {
        return metaAccounts.filter { metaAccount -> metaAccount.hasAccountIn(chain = this) }
    }

    private suspend fun CreateMetaAccountsResult.findDeactivated(oldProxies: List<ProxyAccountLocal>): List<Long> {
        val oldIds = oldProxies.map { it.proxiedMetaId }
        val deactivated = oldIds - alreadyExistedMetaIds

        val alreadyDeactivatedMetaIdsInCache = accountDao.getMetaAccountIdsByStatus(MetaAccountLocal.Status.DEACTIVATED)

        return deactivated - alreadyDeactivatedMetaIdsInCache.toSet()
    }

    private suspend fun List<Long>.takeNotYetDeactivatedMetaAccounts(): List<Long> {
        val alreadyDeactivatedMetaAccountIds = accountDao.getMetaAccountIdsByStatus(MetaAccountLocal.Status.DEACTIVATED)

        return this - alreadyDeactivatedMetaAccountIds.toSet()
    }
}
