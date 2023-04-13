package io.novafoundation.nova.feature_external_sign_api.model.signPayload.evm

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

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
        val symbol: String,
        val decimals: Int
    ) : Parcelable
}
