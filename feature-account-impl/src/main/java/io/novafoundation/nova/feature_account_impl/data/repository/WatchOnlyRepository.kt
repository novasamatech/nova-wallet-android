package io.novafoundation.nova.feature_account_impl.data.repository

import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.core_db.model.chain.ChainAccountLocal
import io.novafoundation.nova.core_db.model.chain.MetaAccountLocal
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
                name = "\uD83C\uDF0C Nova Foundation \uD83C\uDF0C",
                substrateAddress = "H3cSNjAW86NfTecufGPhxutea8KrEcZdPa4XgVKfhTMugej",
                evmAddress = null
            ),
            WatchWalletSuggestion(
                name = "✨\uD83D\uDC4D✨ Day7 ✨\uD83D\uDC4D✨",
                substrateAddress = "Day71GSJAxUUiFic8bVaWoAczR3Ue3jNonBZthVHp2BKzyJ",
                evmAddress = null
            )
        )
    }
}
