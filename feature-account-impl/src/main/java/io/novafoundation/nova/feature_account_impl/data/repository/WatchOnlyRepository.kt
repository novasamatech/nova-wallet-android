package io.novafoundation.nova.feature_account_impl.data.repository

import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.core_db.model.chain.account.ChainAccountLocal
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.runtime.AccountId

class WatchWalletSuggestion(
    val name: String,
    val substrateAddress: String,
    val evmAddress: String?
)

interface WatchOnlyRepository {

    suspend fun changeWatchChainAccount(
        metaId: Long,
        chainId: ChainId,
        accountId: AccountId
    )

    suspend fun addWatchWallet(
        name: String,
        substrateAccountId: AccountId,
        ethereumAccountId: AccountId?
    ): Long

    suspend fun watchWalletSuggestions(): List<WatchWalletSuggestion>
}

class RealWatchOnlyRepository(
    private val accountDao: MetaAccountDao
) : WatchOnlyRepository {

    override suspend fun changeWatchChainAccount(
        metaId: Long,
        chainId: ChainId,
        accountId: AccountId
    ) {
        val chainAccount = ChainAccountLocal(
            metaId = metaId,
            chainId = chainId,
            accountId = accountId,
            cryptoType = null,
            publicKey = null
        )

        accountDao.insertChainAccount(chainAccount)
    }

    override suspend fun addWatchWallet(
        name: String,
        substrateAccountId: AccountId,
        ethereumAccountId: AccountId?
    ): Long {
        val metaAccount = MetaAccountLocal(
            substratePublicKey = null,
            substrateCryptoType = null,
            substrateAccountId = substrateAccountId,
            ethereumPublicKey = null,
            ethereumAddress = ethereumAccountId,
            name = name,
            isSelected = false,
            position = accountDao.nextAccountPosition(),
            type = MetaAccountLocal.Type.WATCH_ONLY
        )

        return accountDao.insertMetaAccount(metaAccount)
    }

    override suspend fun watchWalletSuggestions(): List<WatchWalletSuggestion> {
        return listOf(
            WatchWalletSuggestion(
                name = "\uD83C\uDF0C NOVA",
                substrateAddress = "1ChFWeNRLarAPRCTM3bfJmncJbSAbSS9yqjueWz7jX7iTVZ",
                evmAddress = "0x7Aa98AEb3AfAcf10021539d5412c7ac6AfE0fb00"
            )
        )
    }
}
