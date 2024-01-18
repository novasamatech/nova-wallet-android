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
import io.novafoundation.nova.feature_account_api.data.model.ProxiedWithProxy
import io.novafoundation.nova.feature_account_api.data.proxy.MetaAccountsUpdatesRegistry
import io.novafoundation.nova.feature_account_api.data.proxy.ProxySyncService
import io.novafoundation.nova.feature_account_api.data.repository.ProxyRepository
import io.novafoundation.nova.feature_account_api.domain.account.identity.Identity
import io.novafoundation.nova.feature_account_api.domain.account.identity.IdentityProvider
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccountId
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.proxied.ProxiedAddAccountRepository
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.findChains
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class RealProxySyncService(
    private val chainRegistry: ChainRegistry,
    private val proxyRepository: ProxyRepository,
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

    private suspend fun syncChainProxies(chain: Chain, metaAccounts: List<MetaAccount>) = runCatching {
        Log.d(LOG_TAG, "Started syncing proxies for ${chain.name}")

        val availableAccountIds = chain.getAvailableAccountIds(metaAccounts)

        val proxiedsWithProxies = proxyRepository.getAllProxiesForMetaAccounts(chain.id, availableAccountIds)

        val oldProxies = accountDao.getProxyAccounts(chain.id)

        val notAddedProxies = filterNotAddedProxieds(proxiedsWithProxies, oldProxies)

        val identitiesByChain = notAddedProxies.loadProxiedIdentities(chain.id)
        val addedProxiedsMetaIds = notAddedProxies.map {
            val identity = identitiesByChain[it.proxied.accountId.intoKey()]

            proxiedAddAccountRepository.addAccount(ProxiedAddAccountRepository.Payload(it, identity))
        }

        val deactivatedMetaAccountIds = getDeactivatedMetaIds(proxiedsWithProxies, oldProxies)
        accountDao.changeAccountsStatus(deactivatedMetaAccountIds, MetaAccountLocal.Status.DEACTIVATED)

        val changedMetaIds = addedProxiedsMetaIds + deactivatedMetaAccountIds
        metaAccountsUpdatesRegistry.addMetaIds(changedMetaIds)
    }.onFailure {
        Log.e(LOG_TAG, "Failed to sync proxy delegators in chain ${chain.name}", it)
    }.onSuccess {
        Log.d(LOG_TAG, "Finished syncing proxies for ${chain.name}")
    }

    private fun filterNotAddedProxieds(
        proxiedsWithProxies: List<ProxiedWithProxy>,
        oldProxies: List<ProxyAccountLocal>
    ): List<ProxiedWithProxy> {
        val oldIdentifiers = oldProxies.mapToSet { it.identifier }
        return proxiedsWithProxies.filter { it.toLocalIdentifier() !in oldIdentifiers }
    }

    private suspend fun getDeactivatedMetaIds(
        onChainProxies: List<ProxiedWithProxy>,
        oldProxies: List<ProxyAccountLocal>
    ): List<Long> {
        val newIdentifiers = onChainProxies.mapToSet { it.toLocalIdentifier() }
        val accountsToDeactivate = oldProxies.filter { it.identifier !in newIdentifiers }
            .map { it.proxiedMetaId }

        return accountsToDeactivate.takeNotYetDeactivatedMetaAccounts()
    }

    private suspend fun getMetaAccounts(): List<MetaAccount> {
        return accountRepository.getActiveMetaAccounts()
            .filter { it.isAllowedToSyncProxy() }
    }

    private fun MetaAccount.isAllowedToSyncProxy(): Boolean {
        return when (type) {
            LightMetaAccount.Type.SECRETS,
            LightMetaAccount.Type.PARITY_SIGNER,
            LightMetaAccount.Type.LEDGER,
            LightMetaAccount.Type.POLKADOT_VAULT -> true

            LightMetaAccount.Type.WATCH_ONLY -> shouldSyncWatchOnlyProxies

            LightMetaAccount.Type.PROXIED -> false
        }
    }

    private suspend fun getSupportedProxyChains(): List<Chain> {
        return chainRegistry.findChains { it.supportProxy }
    }

    private fun Chain.getAvailableAccountIds(metaAccounts: List<MetaAccount>): List<MetaAccountId> {
        return metaAccounts.mapNotNull { metaAccount ->
            val accountId = metaAccount.accountIdIn(chain = this)
            accountId?.let {
                MetaAccountId(accountId, metaAccount.id)
            }
        }
    }

    private suspend fun List<ProxiedWithProxy>.loadProxiedIdentities(chainId: ChainId): Map<AccountIdKey, Identity?> {
        val proxiedAccountIds = map { it.proxied.accountId }

        return identityProvider.identitiesFor(proxiedAccountIds, chainId)
    }

    private suspend fun List<Long>.takeNotYetDeactivatedMetaAccounts(): List<Long> {
        val alreadyDeactivatedMetaAccountIds = accountDao.getMetaAccountIdsByStatus(MetaAccountLocal.Status.DEACTIVATED)

        return this - alreadyDeactivatedMetaAccountIds.toSet()
    }

    private fun ProxiedWithProxy.toLocalIdentifier(): String {
        return ProxyAccountLocal.makeIdentifier(
            proxyMetaId = proxy.metaId,
            chainId = proxied.chainId,
            proxiedAccountId = proxied.accountId,
            proxyType = proxy.proxyType
        )
    }
}
