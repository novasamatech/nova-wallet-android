package io.novafoundation.nova.feature_account_impl.data.repository.addAccount.watchOnly

import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.core_db.model.chain.account.ChainAccountLocal
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import io.novafoundation.nova.feature_account_api.data.proxy.ProxySyncService
import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.AddAccountResult
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.BaseAddAccountRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.runtime.AccountId

class WatchOnlyAddAccountRepository(
    private val accountDao: MetaAccountDao,
    proxySyncService: ProxySyncService,
    metaAccountChangesEventBus: MetaAccountChangesEventBus
) : BaseAddAccountRepository<WatchOnlyAddAccountRepository.Payload>(proxySyncService, metaAccountChangesEventBus) {

    sealed interface Payload {
        class MetaAccount(
            val name: String,
            val substrateAccountId: AccountId,
            val ethereumAccountId: AccountId?
        ) : Payload

        class ChainAccount(
            val metaId: Long,
            val chainId: ChainId,
            val accountId: AccountId
        ) : Payload
    }

    override suspend fun addAccountInternal(payload: Payload): AddAccountResult {
        return when (payload) {
            is Payload.MetaAccount -> addWatchOnlyWallet(payload)
            is Payload.ChainAccount -> changeWatchOnlyChainAccount(payload)
        }
    }

    private suspend fun addWatchOnlyWallet(payload: Payload.MetaAccount): AddAccountResult {
        val metaAccount = MetaAccountLocal(
            substratePublicKey = null,
            substrateCryptoType = null,
            substrateAccountId = payload.substrateAccountId,
            ethereumPublicKey = null,
            ethereumAddress = payload.ethereumAccountId,
            name = payload.name,
            parentMetaId = null,
            isSelected = false,
            position = accountDao.nextAccountPosition(),
            type = MetaAccountLocal.Type.WATCH_ONLY,
            status = MetaAccountLocal.Status.ACTIVE
        )

        val metaId = accountDao.insertMetaAccount(metaAccount)

        return AddAccountResult.AccountAdded(metaId)
    }

    private suspend fun changeWatchOnlyChainAccount(payload: Payload.ChainAccount): AddAccountResult {
        val chainAccount = ChainAccountLocal(
            metaId = payload.metaId,
            chainId = payload.chainId,
            accountId = payload.accountId,
            cryptoType = null,
            publicKey = null
        )

        accountDao.insertChainAccount(chainAccount)

        return AddAccountResult.AccountChanged(payload.metaId)
    }
}
