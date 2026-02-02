package io.novafoundation.nova.feature_account_impl.data.repository

class WatchWalletSuggestion(
    val name: String,
    val substrateAddress: String,
    val evmAddress: String
)

interface WatchOnlyRepository {

    fun watchOnlyDemoAccount(): WatchWalletSuggestion
}

class RealWatchOnlyRepository() : WatchOnlyRepository {

    override fun watchOnlyDemoAccount(): WatchWalletSuggestion {
        return WatchWalletSuggestion(
                name = "NOVA DEMO WALLET",
                substrateAddress = "1ChFWeNRLarAPRCTM3bfJmncJbSAbSS9yqjueWz7jX7iTVZ",
                evmAddress = "0x7Aa98AEb3AfAcf10021539d5412c7ac6AfE0fb00"
            )
    }
}
