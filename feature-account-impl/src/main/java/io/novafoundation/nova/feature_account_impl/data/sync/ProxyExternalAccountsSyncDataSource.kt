package io.novafoundation.nova.feature_account_impl.data.sync

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.memory.SingleValueCache
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.buildMultiMapList
import io.novafoundation.nova.common.utils.put
import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.core_db.model.chain.account.ChainAccountLocal
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import io.novafoundation.nova.core_db.model.chain.account.ProxyAccountLocal
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.AddAccountResult
import io.novafoundation.nova.feature_account_api.domain.account.identity.Identity
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.ProxiedMetaAccount
import io.novafoundation.nova.feature_proxy_api.data.repository.GetProxyRepository
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyType
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import javax.inject.Inject

@FeatureScope
internal class ProxyAccountsSyncDataSourceFactory @Inject constructor(
    private val getProxyRepository: GetProxyRepository,
    private val accountDao: MetaAccountDao,
) : ExternalAccountsSyncDataSource.Factory {

    override fun create(chain: Chain): ExternalAccountsSyncDataSource? {
        return if (chain.supportProxy) {
            ProxyExternalAccountsSyncDataSource(getProxyRepository, accountDao, chain)
        } else {
            null
        }
    }
}

private class ProxyExternalAccountsSyncDataSource(
    private val getProxyRepository: GetProxyRepository,
    private val accountDao: MetaAccountDao,
    private val chain: Chain,
) : ExternalAccountsSyncDataSource {

    private val proxiedsByProxy = SingleValueCache {
        fetchProxiedsByProxy()
    }

    override suspend fun isCreatedFromDataSource(metaAccount: MetaAccount): Boolean {
        return metaAccount is ProxiedMetaAccount
    }

    override suspend fun getExternalCreatedAccount(metaAccount: MetaAccount): ExternalSourceCreatedAccount? {
        return if (isCreatedFromDataSource(metaAccount)) {
            ProxyExternalSourceAccount()
        } else {
            null
        }
    }

    override suspend fun getControllableExternalAccounts(accountIdsToQuery: Set<AccountIdKey>): List<ExternalControllableAccount> {
        return accountIdsToQuery.flatMap { proxyCandidate ->
            val proxieds = proxiedsByProxy()[proxyCandidate] ?: return@flatMap emptyList()

            proxieds.map { proxied ->
                ProxiedExternalAccount(
                    accountId = proxied.proxiedAccountId,
                    controllerAccountId = proxyCandidate,
                    proxyType = proxied.proxyType
                )
            }
        }
    }

    private suspend fun fetchProxiedsByProxy(): Map<AccountIdKey, List<ProxyPermission>> {
        val allProxiesByProxied = getProxyRepository.getAllProxies(chain.id)

        return buildMultiMapList {
            allProxiesByProxied.forEach { (proxied, proxies) ->
                proxies.proxies.forEach { proxy ->
                    val proxyPermission = ProxyPermission(
                        proxiedAccountId = proxied,
                        proxyType = proxy.proxyType
                    )
                    put(proxy.proxy, proxyPermission)
                }
            }
        }
    }

    private class ProxyPermission(
        val proxiedAccountId: AccountIdKey,
        val proxyType: ProxyType
    )

    private inner class ProxiedExternalAccount(
        override val accountId: AccountIdKey,
        override val controllerAccountId: AccountIdKey,
        private val proxyType: ProxyType,
    ) : ExternalControllableAccount {

        override fun isRepresentedBy(localAccount: MetaAccount): Boolean {
            return localAccount is ProxiedMetaAccount && localAccount.proxy.proxyType == proxyType
        }

        override suspend fun addAccount(
            controller: MetaAccount,
            identity: Identity?,
            position: Int
        ): AddAccountResult.AccountAdded {
            val metaId = accountDao.insertProxiedMetaAccount(
                metaAccount = createMetaAccount(controller.id, identity, position),
                chainAccount = { proxiedMetaId -> createChainAccount(proxiedMetaId) },
                proxyAccount = { proxiedMetaId -> createProxyAccount(proxiedMetaId = proxiedMetaId, proxyMetaId = controller.id) }
            )

            return AddAccountResult.AccountAdded(metaId, LightMetaAccount.Type.PROXIED)
        }

        override fun dispatchChangesOriginFilters(): Boolean {
            return true
        }

        private fun createMetaAccount(
            controllerMetaId: Long,
            identity: Identity?,
            position: Int
        ): MetaAccountLocal {
            return MetaAccountLocal(
                substratePublicKey = null,
                substrateCryptoType = null,
                substrateAccountId = null,
                ethereumPublicKey = null,
                ethereumAddress = null,
                name = identity?.name ?: chain.addressOf(accountId),
                parentMetaId = controllerMetaId,
                isSelected = false,
                position = position,
                type = MetaAccountLocal.Type.PROXIED,
                status = MetaAccountLocal.Status.ACTIVE,
                globallyUniqueId = MetaAccountLocal.generateGloballyUniqueId(),
                typeExtras = null
            )
        }

        private fun createChainAccount(
            proxiedMetaId: Long,
        ): ChainAccountLocal {
            return ChainAccountLocal(
                metaId = proxiedMetaId,
                chainId = chain.id,
                publicKey = null,
                accountId = accountId.value,
                cryptoType = null
            )
        }

        private fun createProxyAccount(
            proxiedMetaId: Long,
            proxyMetaId: Long,
        ): ProxyAccountLocal {
            return ProxyAccountLocal(
                proxiedMetaId = proxiedMetaId,
                proxyMetaId = proxyMetaId,
                chainId = chain.id,
                proxiedAccountId = accountId.value,
                proxyType = proxyType.name
            )
        }
    }

    private class ProxyExternalSourceAccount : ExternalSourceCreatedAccount {

        override fun canControl(candidate: ExternalControllableAccount): Boolean {
            return !candidate.dispatchChangesOriginFilters()
        }
    }
}
