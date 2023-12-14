package io.novafoundation.nova.feature_account_impl.data.proxy

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.utils.CollectionDiffer
import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.core_db.model.chain.account.ChainAccountLocal
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import io.novafoundation.nova.core_db.model.chain.account.ProxyAccountLocal
import io.novafoundation.nova.feature_account_api.data.model.ProxiedWithProxies
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

class RealProxySyncService(
    private val chainRegistry: ChainRegistry,
    private val proxyRepository: ProxyRepository,
    private val accounRepository: AccountRepository,
    private val accountDao: MetaAccountDao,
    private val identityProvider: IdentityProvider
) : ProxySyncService {

    override suspend fun startSyncing() {
        if (!accounRepository.hasMetaAccounts()) return

        runCatching {
            val metaAccounts = getMetaAccounts()

            val supportedProxyChains = getSupportedProxyChains()
            val chainsToAccountIds = supportedProxyChains.associateWith { chain -> chain.getAvailableAccountIds(metaAccounts) }

            // proxiedsWithProxies will be usefull when we union differen proxy types to one account
            val proxiedsWithProxies = chainsToAccountIds.flatMap { (chain, accountIds) ->
                proxyRepository.getProxyDelegatorsForAccounts(chain.id, accountIds)
            }

            val newProxies = proxiedsWithProxies.formatToLocalProxies()
            val oldProxies = accountDao.getAllProxyAccounts()

            val proxiesDiff = CollectionDiffer.findDiff(newProxies, oldProxies, forceUseNewItems = false)

            insertMetaAndChainAccounts(proxiesDiff)
            insertProxies(proxiesDiff)
        }
    }

    private suspend fun insertMetaAndChainAccounts(proxiesDiff: CollectionDiffer.Diff<ProxyAccountLocal>) {
        val identitiesByChain = proxiesDiff.added.loadProxiedIdentities()

        val chainAccounts = proxiesDiff.added.map { proxy ->
            val identity = identitiesByChain[proxy.chainId]?.get(proxy.proxiedAccountId.intoKey())
            val proxiedMetaId = accountDao.insertMetaAccountWithNewPosition { nextPosition ->
                createMetaAccount(proxy.chainId, proxy.proxiedAccountId, identity, nextPosition)
            }
            createChainAccount(proxiedMetaId, proxy.chainId, proxy.proxiedAccountId)
        }

        accountDao.insertChainAccounts(chainAccounts)
    }

    private suspend fun insertProxies(proxiesDiff: CollectionDiffer.Diff<ProxyAccountLocal>) {
        val deactivatedProxies = proxiesDiff.removed.map { it.copy(status = ProxyAccountLocal.Status.DEACTIVATED) }

        accountDao.insertProxies(proxiesDiff.newOrUpdated)
        accountDao.insertProxies(deactivatedProxies)
    }

    override suspend fun syncForMetaAccount(metaAccount: MetaAccount) {
        TODO("provide updater to sync proxy delegators for new added accounts")
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

    private suspend fun createMetaAccount(chainId: ChainId, proxiedAccountId: AccountId, identity: Identity?, position: Int): MetaAccountLocal {
        val chain = chainRegistry.getChain(chainId)
        return MetaAccountLocal(
            substratePublicKey = null,
            substrateCryptoType = null,
            substrateAccountId = if (chain.isSubstrateBased) proxiedAccountId else null,
            ethereumPublicKey = null,
            ethereumAddress = if (chain.isEthereumBased) proxiedAccountId else null,
            name = identity?.name ?: chain.addressOf(proxiedAccountId),
            isSelected = false,
            position = position,
            type = MetaAccountLocal.Type.PROXIED,
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
            metaId = metaId,
            chainId = chainId,
            proxiedAccountId = proxiedAccountId,
            proxyType = proxyType,
            status = ProxyAccountLocal.Status.ACTIVE
        )
    }

    private fun List<ProxiedWithProxies>.formatToLocalProxies(): List<ProxyAccountLocal> {
        return flatMap { proxied ->
            proxied.proxies.map { proxy ->
                createProxyAccount(proxy.metaId, proxied.chainId, proxied.accountId, proxy.proxyType)
            }
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
