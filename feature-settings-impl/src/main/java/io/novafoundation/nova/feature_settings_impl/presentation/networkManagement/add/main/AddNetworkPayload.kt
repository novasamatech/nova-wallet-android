package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.add.main

import android.os.Parcelable
import java.math.BigInteger
import kotlinx.android.parcel.Parcelize

@Parcelize
class AddNetworkPayload(
    val mode: Mode,
    val prefilledData: NetworkData?
) : Parcelable {

    enum class Mode {
        SUBSTRATE, EVM
    }

    @Parcelize
    class NetworkData(
        val iconUrl: String?,
        val rpcNodeUrl: String?,
        val networkName: String?,
        val tokenName: String?,
        val evmChainId: BigInteger?,
        val blockExplorerUrl: String?,
        val coingeckoLink: String?
    ) : Parcelable
}
