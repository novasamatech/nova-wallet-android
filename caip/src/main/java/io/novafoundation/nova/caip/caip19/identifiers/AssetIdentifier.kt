package io.novafoundation.nova.caip.caip19.identifiers

const val SLIP44 = "slip44"
const val ERC20 = "erc20"

sealed interface AssetIdentifier {
    class Slip44(val slip44CoinCode: Int) : AssetIdentifier

    class Erc20(val contractAddress: String) : AssetIdentifier
}
