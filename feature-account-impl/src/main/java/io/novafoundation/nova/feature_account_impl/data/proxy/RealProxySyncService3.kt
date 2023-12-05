package io.novafoundation.nova.feature_account_impl.data.proxy

import android.util.Log
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.utils.CollectionDiffer
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.core_db.model.chain.account.ChainAccountLocal
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import io.novafoundation.nova.core_db.model.chain.account.ProxyAccountLocal
import io.novafoundation.nova.feature_account_api.data.model.ProxiedWithProxy
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
import io.novafoundation.nova.runtime.ext.isSubstrateBased
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.findChains
import jp.co.soramitsu.fearless_utils.runtime.AccountId

class RealProxySyncService2(
    private val chainRegistry: ChainRegistry,
    private val proxyRepository: ProxyRepository,
    private val accounRepository: AccountRepository,
    private val accountDao: MetaAccountDao,
    private val identityProvider: IdentityProvider
) : ProxySyncService {

    override suspend fun startSyncing() {
        val metaAccounts = getMetaAccounts()
        if (metaAccounts.isEmpty()) return

        try {
            val supportedProxyChains = getSupportedProxyChains()
            val chainsToAccountIds = supportedProxyChains.associateWith { chain -> chain.getAvailableAccountIds(metaAccounts) }

            val proxiedsWithProxies = chainsToAccountIds.flatMap { (chain, accountIds) ->
                proxyRepository.getProxyDelegatorsForAccounts(chain.id, accountIds)
            }

            val newProxies = proxiedsWithProxies.formatToLocalProxies()
            val oldProxies = accountDao.getAllProxyAccounts()

            val proxiesDiff = CollectionDiffer.findDiff(newProxies, oldProxies, forceUseNewItems = false)

            insertMetaAndChainAccounts(proxiesDiff)
            insertProxies(proxiesDiff)
            deactivateRemovedProxies(metaAccounts, proxiesDiff)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to sync proxies", e)
        }
    }

    override suspend fun syncForMetaAccount(metaAccount: MetaAccount) {
        TODO("provide updater to sync proxy delegators for new added accounts")
    }

    private suspend fun deactivateRemovedProxies(metaAccounts: List<MetaAccount>, proxiesDiff: CollectionDiffer.Diff<ProxyAccountLocal>) {
        val proxiesToDeactivate = proxiesDiff.removed.map { it.proxyMetaId }
        val metaAccountsToDeactivate = metaAccounts.filter { it.proxy?.metaId in proxiesToDeactivate }
            .map { it.id }

        accountDao.changeAccountsStatus(metaAccountsToDeactivate, MetaAccountLocal.Status.DEACTIVATED)
    }

    private suspend fun insertMetaAndChainAccounts(proxiesDiff: CollectionDiffer.Diff<ProxyAccountLocal>) {
        val identitiesByChain = proxiesDiff.added.loadProxiedIdentities()

        val chainAccounts = proxiesDiff.added.map { proxy ->
            val identity = identitiesByChain[proxy.chainId]?.get(proxy.proxiedAccountId.intoKey())
            val proxiedMetaId = accountDao.insertMetaAccountWithNewPosition { nextPosition ->
                createMetaAccount(proxy.chainId, proxy.proxyMetaId, proxy.proxiedAccountId, identity, nextPosition)
            }
            createChainAccount(proxiedMetaId, proxy.chainId, proxy.proxiedAccountId)
        }

        accountDao.insertChainAccounts(chainAccounts)
    }

    private suspend fun insertProxies(proxiesDiff: CollectionDiffer.Diff<ProxyAccountLocal>) {
        accountDao.insertProxies(proxiesDiff.newOrUpdated)
    }

    private suspend fun getMetaAccounts(): List<MetaAccount> {
        return accounRepository.allMetaAccounts()
            .filter {
                when (it.type) {
                    LightMetaAccount.Type.SECRETS,
                    LightMetaAccount.Type.PARITY_SIGNER,
                    LightMetaAccount.Type.LEDGER,
                    LightMetaAccount.Type.POLKADOT_VAULT -> true

                    LightMetaAccount.Type.WATCH_ONLY,
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
        chainId: ChainId,
        parentMetaId: Long,
        proxiedAccountId: AccountId,
        identity: Identity?,
        position: Int
    ): MetaAccountLocal {
        val chain = chainRegistry.getChain(chainId)
        return MetaAccountLocal(
            substratePublicKey = null,
            substrateCryptoType = null,
            substrateAccountId = if (chain.isSubstrateBased) proxiedAccountId else null,
            ethereumPublicKey = null,
            ethereumAddress = if (chain.isEthereumBased) proxiedAccountId else null,
            name = identity?.name ?: chain.addressOf(proxiedAccountId),
            parentMetaId = parentMetaId,
            isSelected = false,
            position = position,
            type = MetaAccountLocal.Type.PROXIED,
            status = MetaAccountLocal.Status.ACTIVE
        )
    }

    private suspend fun createChainAccount(metaId: Long, chainId: ChainId, accountId: AccountId): ChainAccountLocal {
        return ChainAccountLocal(
            metaId = metaId,
            chainId = chainId,
            publicKey = null,
            accountId = accountId,
            cryptoType = null
        )
    }

    private fun createProxyAccount(
        metaId: Long,
        chainId: ChainId,
        proxiedAccountId: AccountId,
        proxyType: String
    ): ProxyAccountLocal {
        return ProxyAccountLocal(
            proxiedMetaId = metaId,
            proxyMetaId = metaId,
            chainId = chainId,
            proxiedAccountId = proxiedAccountId,
            proxyType = proxyType
        )
    }

    private fun List<ProxiedWithProxy>.formatToLocalProxies(): List<ProxyAccountLocal> {
        return map {
            createProxyAccount(it.proxy.metaId, it.proxied.chainId, it.proxied.accountId, it.proxy.proxyType)
        }
    }

    private suspend fun List<ProxyAccountLocal>.loadProxiedIdentities(): Map<ChainId, Map<AccountIdKey, Identity?>> {
        return this.groupBy { it.chainId }
            .mapValues { (chainId, proxiesWithProxieds) ->
                val proxiedAccountIds = proxiesWithProxieds.map { it.proxiedAccountId }
                identityProvider.identitiesFor(proxiedAccountIds, chainId)
            }
    }
}
