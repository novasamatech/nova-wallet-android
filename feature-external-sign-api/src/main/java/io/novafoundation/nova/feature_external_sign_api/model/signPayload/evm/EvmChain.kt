package io.novafoundation.nova.feature_external_sign_api.model.signPayload.evm

import android.os.Parcelable
import io.novafoundation.nova.common.utils.Precision
import io.novafoundation.nova.common.utils.TokenSymbol
import kotlinx.parcelize.Parcelize

@Parcelize
data class EvmChain(
    val chainId: String,
    val chainName: String,
    val nativeCurrency: NativeCurrency,
    val rpcUrl: String,
    val iconUrl: String?
) : Parcelable {

    @Parcelize
    class NativeCurrency(
        val name: String,
        val symbol: TokenSymbol,
        val decimals: Precision
    ) : Parcelable
}
