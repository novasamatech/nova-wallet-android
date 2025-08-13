package io.novafoundation.nova.feature_account_impl.data.sync

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.core_db.model.chain.account.ChainAccountLocal
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import io.novafoundation.nova.core_db.model.chain.account.ProxyAccountLocal
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.AddAccountResult
import io.novafoundation.nova.feature_account_api.domain.account.identity.Identity
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.ProxiedMetaAccount
import io.novafoundation.nova.feature_account_impl.data.proxy.repository.MultiChainProxyRepository
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyType
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainsById
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.findChainsById
import javax.inject.Inject

@FeatureScope
internal class ProxyAccountsSyncDataSourceFactory @Inject constructor(
    private val multiChainProxyRepository: MultiChainProxyRepository,
    private val accountDao: MetaAccountDao,
    private val chainRegistry: ChainRegistry
) : ExternalAccountsSyncDataSource.Factory {

    override suspend fun create(): ExternalAccountsSyncDataSource {
        val proxyChains = chainRegistry.findChainsById { it.supportProxy }

        return ProxyExternalAccountsSyncDataSource(multiChainProxyRepository, accountDao, proxyChains)
    }
}

private class ProxyExternalAccountsSyncDataSource(
    private val multiChainProxyRepository: MultiChainProxyRepository,
    private val accountDao: MetaAccountDao,
    private val proxyChains: ChainsById
) : ExternalAccountsSyncDataSource {

    override fun supportedChains(): Collection<Chain> {
        return proxyChains.values
    }

    override suspend fun isCreatedFromDataSource(metaAccount: MetaAccount): Boolean {
        return metaAccount is ProxiedMetaAccount
    }

    override suspend fun getExternalCreatedAccount(metaAccount: MetaAccount): ExternalSourceCreatedAccount? {
        return if (metaAccount is ProxiedMetaAccount) {
            ProxyExternalSourceAccount(metaAccount.proxy.proxyType)
        } else {
            null
        }
    }

    override suspend fun getControllableExternalAccounts(accountIdsToQuery: Set<AccountIdKey>): List<ExternalControllableAccount> {
        return multiChainProxyRepository.getProxies(accountIdsToQuery)
            .mapNotNull {
                ProxiedExternalAccount(
                    chain = proxyChains[it.chainId] ?: return@mapNotNull null,
                    accountId = it.proxied,
                    proxyType = it.proxyType,
                    controllerAccountId = it.proxy
                )
            }
    }

    private inner class ProxiedExternalAccount(
        override val accountId: AccountIdKey,
        override val controllerAccountId: AccountIdKey,
        private val proxyType: ProxyType,
        private val chain: Chain
    ) : ExternalControllableAccount {

        override fun isRepresentedBy(localAccount: MetaAccount): Boolean {
            return localAccount is ProxiedMetaAccount && localAccount.proxy.proxyType == proxyType
        }

        override fun isAvailableOn(chain: Chain): Boolean {
            return chain.id == this.chain.id
        }

        override suspend fun addControlledAccount(
            controller: MetaAccount,
            identity: Identity?,
            position: Int,
            missingAccountChain: Chain
        ): AddAccountResult.AccountAdded {
            require(missingAccountChain.id == chain.id) {
                "Wrong chain requested for ProxiedExternalAccount.addControlledAccount. Expected: ${chain.name}, got: ${missingAccountChain.name}"
            }

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

    private inner class ProxyExternalSourceAccount(private val proxyType: ProxyType) : ExternalSourceCreatedAccount {

        override fun canControl(candidate: ExternalControllableAccount): Boolean {
            if (!candidate.dispatchChangesOriginFilters()) return true

            return proxyType is ProxyType.Any || proxyType is ProxyType.NonTransfer
        }
    }
}
