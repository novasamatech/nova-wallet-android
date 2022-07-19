package io.novafoundation.nova.feature_account_impl.data.repository

import io.novafoundation.nova.core_db.dao.MetaAccountDao
import jp.co.soramitsu.fearless_utils.runtime.AccountId

class WatchWalletSuggestion(
    val name: String,
    val substrateAddress: String,
    val evmAddress: String?
)

interface WatchOnlyRepository {

    suspend fun addWatchWallet(
        name: String,
        substrateAccountId: AccountId,
        ethereumAccountId: AccountId
    )

    suspend fun watchWalletSuggestions(): List<WatchWalletSuggestion>
}

class RealWatchOnlyRepository(
    private val accountDao: MetaAccountDao
): WatchOnlyRepository {

    override suspend fun addWatchWallet(
        name: String,
        substrateAccountId: AccountId,
        ethereumAccountId: AccountId
    ) {
        TODO("Not yet implemented")
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
