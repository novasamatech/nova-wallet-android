package io.novafoundation.nova.feature_account_impl.data.proxy

import android.util.Log
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.mapToSet
import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.core_db.model.chain.account.ChainAccountLocal
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
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.findChains
import jp.co.soramitsu.fearless_utils.runtime.AccountId

class RealProxySyncService(
    private val chainRegistry: ChainRegistry,
    private val proxyRepository: ProxyRepository,
    private val accounRepository: AccountRepository,
    private val accountDao: MetaAccountDao,
    private val identityProvider: IdentityProvider,
    private val metaAccountsUpdatesRegistry: MetaAccountsUpdatesRegistry
) : ProxySyncService {

    override suspend fun startSyncing() {
        val metaAccounts = getMetaAccounts()
        if (metaAccounts.isEmpty()) return

        runCatching {
            val supportedProxyChains = getSupportedProxyChains()

            val chainsToAccountIds = supportedProxyChains.associateWith { chain -> chain.getAvailableAccountIds(metaAccounts) }

            val proxiedsWithProxies = chainsToAccountIds.flatMap { (chain, accountIds) ->
                proxyRepository.getAllProxiesForMetaAccounts(chain.id, accountIds)
            }

            val oldProxies = accountDao.getAllProxyAccounts()

            val notAddedProxies = filterNotAddedProxieds(proxiedsWithProxies, oldProxies)

            val identitiesByChain = notAddedProxies.loadProxiedIdentities()
            val proxiedsToMetaId = notAddedProxies.map {
                val proxied = it.proxied
                val proxy = it.proxy
                val chain = chainRegistry.getChain(proxied.chainId)
                val position = accountDao.nextAccountPosition()
                val identity = identitiesByChain[proxied.chainId]?.get(proxied.accountId.intoKey())

                val proxiedMetaId = accountDao.insertProxiedMetaAccount(
                    metaAccount = createMetaAccount(chain, proxy.metaId, proxied.accountId, identity, position),
                    chainAccount = { createChainAccount(it, proxied.chainId, proxied.accountId) },
                    proxyAccount = { createProxyAccount(it, proxy.metaId, proxied.chainId, proxied.accountId, proxy.proxyType) }
                )

                it to proxiedMetaId
            }

            val deactivatedMetaAccountIds = getDeactivatedMetaIds(proxiedsWithProxies, oldProxies)
            accountDao.changeAccountsStatus(deactivatedMetaAccountIds, MetaAccountLocal.Status.DEACTIVATED)

            val changedMetaIds = proxiedsToMetaId.map { it.second } + deactivatedMetaAccountIds
            metaAccountsUpdatesRegistry.addMetaIds(changedMetaIds)
        }.onFailure {
            Log.e(LOG_TAG, "Failed to sync proxy delegators", it)
        }
    }

    override suspend fun syncForMetaAccount(metaAccount: MetaAccount) {
        TODO("provide updater to sync proxy delegators for new added accounts")
    }

    private suspend fun filterNotAddedProxieds(
        proxiedsWithProxies: List<ProxiedWithProxy>,
        oldProxies: List<ProxyAccountLocal>
    ): List<ProxiedWithProxy> {
        val oldInditifiers = oldProxies.map { it.identifier }.toSet()
        return proxiedsWithProxies.filter { it.toLocalIdentifier() !in oldInditifiers }
    }

    private suspend fun getDeactivatedMetaIds(
        proxiedsWithProxies: List<ProxiedWithProxy>,
        oldProxies: List<ProxyAccountLocal>
    ): List<Long> {
        val newIdentifiers = proxiedsWithProxies.map { it.toLocalIdentifier() }.toSet()
        return oldProxies.filter { it.identifier !in newIdentifiers }
            .map { it.proxiedMetaId }
    }

    private suspend fun getPedsToRemove(
        oldProxies: List<ProxyAccountLocal>,
        proxiedsMetaAccounts: List<MetaAccountLocal>
    ): List<Long> {
        val proxiedsMetaIds = proxiedsMetaAccounts.mapToSet { it.id }

        return oldProxies.filter { it.proxiedMetaId !in proxiedsMetaIds }
            .map { it.proxiedMetaId }
    }

    private suspend fun getMetaAccounts(): List<MetaAccount> {
        return accounRepository.allMetaAccounts()
            .filter {
                when (it.type) {
                    LightMetaAccount.Type.SECRETS,
                    LightMetaAccount.Type.PARITY_SIGNER,
                    LightMetaAccount.Type.LEDGER,
                    LightMetaAccount.Type.POLKADOT_VAULT,
                    LightMetaAccount.Type.WATCH_ONLY -> true

                    LightMetaAccount.Type.PROXIED -> false
                }
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

    private suspend fun createMetaAccount(
        chain: Chain,
        parentMetaId: Long,
        proxiedAccountId: AccountId,
        identity: Identity?,
        position: Int
    ): MetaAccountLocal {
        return MetaAccountLocal(
            substratePublicKey = null,
            substrateCryptoType = null,
            substrateAccountId = null,
            ethereumPublicKey = null,
            ethereumAddress = null,
            name = identity?.name ?: chain.addressOf(proxiedAccountId),
            parentMetaId = parentMetaId,
            isSelected = false,
            position = position,
            type = MetaAccountLocal.Type.PROXIED,
            status = MetaAccountLocal.Status.ACTIVE
        )
    }

    private fun createChainAccount(metaId: Long, chainId: ChainId, accountId: AccountId): ChainAccountLocal {
        return ChainAccountLocal(
            metaId = metaId,
            chainId = chainId,
            publicKey = null,
            accountId = accountId,
            cryptoType = null
        )
    }

    private fun createProxyAccount(
        proxiedMetaId: Long,
        proxyMetaId: Long,
        chainId: ChainId,
        proxiedAccountId: AccountId,
        proxyType: String
    ): ProxyAccountLocal {
        return ProxyAccountLocal(
            proxiedMetaId = proxiedMetaId,
            proxyMetaId = proxyMetaId,
            chainId = chainId,
            proxiedAccountId = proxiedAccountId,
            proxyType = proxyType
        )
    }

    private suspend fun List<ProxiedWithProxy>.loadProxiedIdentities(): Map<ChainId, Map<AccountIdKey, Identity?>> {
        return this.groupBy { it.proxied.chainId }
            .mapValues { (chainId, proxiedWithProxies) ->
                val proxiedAccountIds = proxiedWithProxies.map { it.proxied.accountId }
                identityProvider.identitiesFor(proxiedAccountIds, chainId)
            }
    }

    private fun ProxiedWithProxy.toLocalIdentifier(): String {
        return ProxyAccountLocal.makeIdentifier(
            proxy.metaId,
            proxied.chainId,
            proxied.accountId,
            proxy.proxyType
        )
    }
}
