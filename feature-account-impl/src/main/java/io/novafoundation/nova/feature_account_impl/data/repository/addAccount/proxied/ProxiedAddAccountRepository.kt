package io.novafoundation.nova.feature_account_impl.data.repository.addAccount.proxied

import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.core_db.model.chain.account.ChainAccountLocal
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import io.novafoundation.nova.core_db.model.chain.account.ProxyAccountLocal
import io.novafoundation.nova.feature_account_api.data.model.ProxiedWithProxy
import io.novafoundation.nova.feature_account_api.data.proxy.ProxySyncService
import io.novafoundation.nova.feature_account_api.domain.account.identity.Identity
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.AddAccountRepository
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.runtime.AccountId

/**
 * It's important to extends ProxiedAddAccountRepository from AddAccountRepository instead of BaseAddAccountRepository
 * since we not need to sync proxy accounts for this case
 */
class ProxiedAddAccountRepository(
    private val accountDao: MetaAccountDao,
    private val chainRegistry: ChainRegistry
) : AddAccountRepository<ProxiedAddAccountRepository.Payload> {

    class Payload(
        val proxiedWithProxy: ProxiedWithProxy,
        val identity: Identity?
    )

    override suspend fun addAccount(payload: Payload): Long {
        val proxied = payload.proxiedWithProxy.proxied
        val proxy = payload.proxiedWithProxy.proxy
        val chain = chainRegistry.getChain(proxied.chainId)
        val position = accountDao.nextAccountPosition()

        return accountDao.insertProxiedMetaAccount(
            metaAccount = createMetaAccount(chain, proxy.metaId, proxied.accountId, payload.identity, position),
            chainAccount = { createChainAccount(it, proxied.chainId, proxied.accountId) },
            proxyAccount = { createProxyAccount(it, proxy.metaId, proxied.chainId, proxied.accountId, proxy.proxyType) }
        )
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
}
