package io.novafoundation.nova.feature_account_impl.data.repository.addAccount.proxied

import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.core_db.model.chain.account.ChainAccountLocal
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import io.novafoundation.nova.core_db.model.chain.account.ProxyAccountLocal
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.AddAccountResult
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.proxied.ProxiedAddAccountRepository
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.proxied.ProxiedAddAccountRepository.Payload
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

/**
 * It's important to extends ProxiedAddAccountRepository from AddAccountRepository instead of BaseAddAccountRepository
 * since we don't need to sync proxy accounts for this case
 */
class RealProxiedAddAccountRepository(
    private val accountDao: MetaAccountDao,
    private val chainRegistry: ChainRegistry
) : ProxiedAddAccountRepository {

    override suspend fun addAccount(payload: Payload): AddAccountResult {
        val position = accountDao.nextAccountPosition()

        val metaId = accountDao.insertProxiedMetaAccount(
            metaAccount = createMetaAccount(payload, position),
            chainAccount = { createChainAccount(it, payload) },
            proxyAccount = { createProxyAccount(it, payload) }
        )

        return AddAccountResult.AccountAdded(metaId)
    }

    private suspend fun createMetaAccount(
        payload: Payload,
        position: Int
    ): MetaAccountLocal {
        val chain = chainRegistry.getChain(payload.chainId)

        return MetaAccountLocal(
            substratePublicKey = null,
            substrateCryptoType = null,
            substrateAccountId = null,
            ethereumPublicKey = null,
            ethereumAddress = null,
            name = payload.identity?.name ?: chain.addressOf(payload.proxiedAccountId),
            parentMetaId = payload.proxyMetaId,
            isSelected = false,
            position = position,
            type = MetaAccountLocal.Type.PROXIED,
            status = MetaAccountLocal.Status.ACTIVE,
            globallyUniqueId = MetaAccountLocal.generateGloballyUniqueId()
        )
    }

    private fun createChainAccount(proxiedMetaId: Long, payload: Payload): ChainAccountLocal {
        return ChainAccountLocal(
            metaId = proxiedMetaId,
            chainId = payload.chainId,
            publicKey = null,
            accountId = payload.proxiedAccountId,
            cryptoType = null
        )
    }

    private fun createProxyAccount(
        proxiedMetaId: Long,
        payload: Payload
    ): ProxyAccountLocal {
        return ProxyAccountLocal(
            proxiedMetaId = proxiedMetaId,
            proxyMetaId = payload.proxyMetaId,
            chainId = payload.chainId,
            proxiedAccountId = payload.proxiedAccountId,
            proxyType = payload.proxyType.name
        )
    }
}
