package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit

/** iOS `SubtensorSubnetNaming.symbol`: the subnet's alpha symbol, "SN<netuid>" when it has none. */
internal fun bittensorAlphaSymbol(netuid: Int, symbol: String?): String = symbol?.takeIf { it.isNotBlank() } ?: "SN$netuid"
