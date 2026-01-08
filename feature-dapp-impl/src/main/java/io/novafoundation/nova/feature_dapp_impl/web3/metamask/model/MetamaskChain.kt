package io.novafoundation.nova.feature_dapp_impl.web3.metamask.model

import android.os.Parcelable
import io.novafoundation.nova.common.utils.removeHexPrefix
import io.novafoundation.nova.runtime.BuildConfig
import kotlinx.parcelize.Parcelize

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
            rpcUrls = listOf("https://mainnet.infura.io/v3/${BuildConfig.INFURA_API_KEY}"),
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
