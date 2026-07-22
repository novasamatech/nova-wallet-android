package io.novafoundation.nova.common.data.analytics

object AssetCategoryClassifier {

    private val STABLECOINS = setOf(
        "USDT", "USDC", "DAI", "BUSD", "TUSD", "FRAX", "LUSD",
        "USDP", "GUSD", "USDD", "CRVUSD", "GHO", "PYUSD",
        "AUSD", "IUSD"
    )

    private val WRAPPED_TOKENS = setOf(
        "WETH", "WBTC", "WBNB", "WAVAX", "WMATIC", "WFTM",
        "WGLMR", "WMOVR", "WDOT", "WKSM"
    )

    private val NATIVE_TOKENS = setOf(
        "DOT", "KSM", "ETH", "BTC", "BNB", "AVAX", "MATIC",
        "SOL", "FTM", "GLMR", "MOVR", "ASTR", "ACA", "CFG",
        "HDX", "INTR", "KINT", "PHA", "ZTG", "NODL", "RING",
        "TEER", "TUR", "UNQ", "AZERO"
    )

    fun classify(symbol: String): AssetCategory {
        val upperSymbol = symbol.uppercase()

        return when {
            NATIVE_TOKENS.contains(upperSymbol) -> AssetCategory.NATIVE_TOKEN
            STABLECOINS.contains(upperSymbol) -> AssetCategory.STABLECOIN
            WRAPPED_TOKENS.contains(upperSymbol) -> AssetCategory.WRAPPED_TOKEN
            upperSymbol.startsWith("W") && NATIVE_TOKENS.contains(upperSymbol.removePrefix("W")) -> AssetCategory.WRAPPED_TOKEN
            else -> AssetCategory.OTHER
        }
    }
}
