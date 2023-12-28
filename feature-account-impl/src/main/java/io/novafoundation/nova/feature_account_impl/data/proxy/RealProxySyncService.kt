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
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.proxied.ProxiedAddAccountRepository.Payload
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.findChains
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val SYNC_TIMEOUT = 10_000L // 10 seconds

class RealProxySyncService(
    private val chainRegistry: ChainRegistry,
    private val proxyRepository: ProxyRepository,
    private val accounRepository: AccountRepository,
    private val accountDao: MetaAccountDao,
    private val identityProvider: IdentityProvider,
    private val metaAccountsUpdatesRegistry: MetaAccountsUpdatesRegistry,
    private val proxiedAddAccountRepository: ProxiedAddAccountRepository,
    private val rootScope: RootScope
) : ProxySyncService {

    override fun startSyncing() {
        rootScope.launch(Dispatchers.Default) {
            startSyncInternal()
        }
    }

    private suspend fun startSyncInternal() {
        val metaAccounts = getMetaAccounts()
        if (metaAccounts.isEmpty()) return

        runCatching {
            val supportedProxyChains = getSupportedProxyChains()
            val chainsToAccountIds = supportedProxyChains.associateWith { chain -> chain.getAvailableAccountIds(metaAccounts) }

            val proxiedsWithProxies = chainsToAccountIds.flatMap { (chain, accountIds) ->
                withTimeoutOrNull(SYNC_TIMEOUT) {
                    proxyRepository.getAllProxiesForMetaAccounts(chain.id, accountIds)
                }.orEmpty()
            }

            val oldProxies = accountDao.getAllProxyAccounts()

            val notAddedProxies = filterNotAddedProxieds(proxiedsWithProxies, oldProxies)

            val identitiesByChain = notAddedProxies.loadProxiedIdentities()
            val addedProxiedsIds = notAddedProxies.map {
                val identity = identitiesByChain[it.proxied.chainId]?.get(it.proxied.accountId.intoKey())

                val proxiedMetaId = proxiedAddAccountRepository.addAccount(Payload(it, identity))

                it to proxiedMetaId
            }

            val deactivatedMetaAccountIds = getDeactivatedMetaIds(proxiedsWithProxies, oldProxies)
            accountDao.changeAccountsStatus(deactivatedMetaAccountIds, MetaAccountLocal.Status.DEACTIVATED)

            val changedMetaIds = addedProxiedsIds.map { it.second } + deactivatedMetaAccountIds
            metaAccountsUpdatesRegistry.addMetaIds(changedMetaIds)
        }.onFailure {
            Log.e(LOG_TAG, "Failed to sync proxy delegators", it)
        }
    }

    private suspend fun filterNotAddedProxieds(
        proxiedsWithProxies: List<ProxiedWithProxy>,
        oldProxies: List<ProxyAccountLocal>
    ): List<ProxiedWithProxy> {
        val oldInditifiers = oldProxies.map { it.identifier }.toSet()
        return proxiedsWithProxies.filter { it.toLocalIdentifier() !in oldInditifiers }
    }

    private suspend fun getDeactivatedMetaIds(
        onChainProxies: List<ProxiedWithProxy>,
        oldProxies: List<ProxyAccountLocal>
    ): List<Long> {
        val newIdentifiers = onChainProxies.map { it.toLocalIdentifier() }.toSet()
        val accountsToDeactivate = oldProxies.filter { it.identifier !in newIdentifiers }
            .map { it.proxiedMetaId }

        return accountsToDeactivate.takeNotYetDeactivatedMetaAccounts()
    }

    private suspend fun getProxiedsToRemove(
        oldProxies: List<ProxyAccountLocal>,
        proxiedsMetaAccounts: List<MetaAccountLocal>
    ): List<Long> {
        val proxiedsMetaIds = proxiedsMetaAccounts.mapToSet { it.id }

        return oldProxies.filter { it.proxiedMetaId !in proxiedsMetaIds }
            .map { it.proxiedMetaId }
    }

    private suspend fun getMetaAccounts(): List<MetaAccount> {
        return accounRepository.allMetaAccounts()
            .filter { it.isAllowedToSyncProxy() }
    }

    private fun MetaAccount.isAllowedToSyncProxy(): Boolean {
        return when (type) {
            LightMetaAccount.Type.SECRETS,
            LightMetaAccount.Type.PARITY_SIGNER,
            LightMetaAccount.Type.LEDGER,
            LightMetaAccount.Type.POLKADOT_VAULT,
            LightMetaAccount.Type.WATCH_ONLY -> true

            LightMetaAccount.Type.PROXIED -> false
        }
    }

    private suspend fun getSupportedProxyChains(): List<Chain> {
        return chainRegistry.findChains { it.supportProxy }
    }

    private suspend fun Chain.getAvailableAccountIds(metaAccounts: List<MetaAccount>): List<MetaAccountId> {
        return metaAccounts.mapNotNull { metaAccount ->
            val accountId = metaAccount.accountIdIn(chain = this)
            accountId?.let {
                MetaAccountId(accountId, metaAccount.id)
            }
        }
    }

    private suspend fun List<ProxiedWithProxy>.loadProxiedIdentities(): Map<ChainId, Map<AccountIdKey, Identity?>> {
        return this.groupBy { it.proxied.chainId }
            .mapValues { (chainId, proxiedWithProxies) ->
                val proxiedAccountIds = proxiedWithProxies.map { it.proxied.accountId }
                identityProvider.identitiesFor(proxiedAccountIds, chainId)
            }
    }

    private suspend fun List<Long>.takeNotYetDeactivatedMetaAccounts(): List<Long> {
        val alreadyDeactivatedMetaAccountIds = accountDao.getMetaAccountsByStatus(MetaAccountLocal.Status.DEACTIVATED)
            .mapToSet { it.id }

        return this - alreadyDeactivatedMetaAccountIds
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
