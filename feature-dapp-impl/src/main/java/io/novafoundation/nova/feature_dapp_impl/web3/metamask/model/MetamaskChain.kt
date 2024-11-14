package io.novafoundation.nova.feature_dapp_impl.web3.metamask.model

import android.os.Parcelable
import io.novafoundation.nova.common.utils.removeHexPrefix
import kotlinx.android.parcel.Parcelize

@Parcelize
data class MetamaskChain(
    val chainId: String,
    val chainName: String,
    val nativeCurrency: NativeCurrency,
    val rpcUrls: List<String>,
    val blockExplorerUrls: List<String>?,
    val iconUrls: List<String>?
) : Parcelable {

    companion object {

        val ETHEREUM = MetamaskChain(
            chainId = "0x1",
            chainName = "Ethereum Mainnet",
            nativeCurrency = NativeCurrency(name = "Ether", symbol = "ETH", decimals = 18),
            rpcUrls = listOf("https://mainnet.infura.io/v3/6b7733290b9a4156bf62a4ba105b76ec"),
            blockExplorerUrls = null,
            iconUrls = null
        )

        val MOONBEAM = MetamaskChain(
            chainId = "0x504",
            chainName = "Moonbeam Network",
            nativeCurrency = NativeCurrency(name = "GLMR", symbol = "GLMR", decimals = 18),
            rpcUrls = listOf("https://endpoints.omniatech.io/v1/moonbeam/mainnet/public"),
            blockExplorerUrls = null,
            iconUrls = null
        )
    }

    @Parcelize
    class NativeCurrency(
        val name: String,
        val symbol: String,
        val decimals: Int
    ) : Parcelable
}

fun MetamaskChain.chainIdInt(): Int = chainId.removeHexPrefix().toInt(16)
