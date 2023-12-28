package io.novafoundation.nova.feature_account_impl.data.repository

import io.novafoundation.nova.core_db.dao.MetaAccountDao

class WatchWalletSuggestion(
    val name: String,
    val substrateAddress: String,
    val evmAddress: String?
)

interface WatchOnlyRepository {

    suspend fun watchWalletSuggestions(): List<WatchWalletSuggestion>
}

class RealWatchOnlyRepository(
    private val accountDao: MetaAccountDao
) : WatchOnlyRepository {

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
