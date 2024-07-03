package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.add.networkDetails

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize


@Parcelize
class AddNetworkPayload(
    val mode: Mode,
    val prefilledData: NetworkData?
) : Parcelable {

    enum class Mode {
        Substrate, EVM
    }

    @Parcelize
    class NetworkData(
        val rpcNodeUrl: String,
        val networkName: String,
        val tokenName: String,
        val chainId: String?,
        val blockExplorerUrl: String?,
        val coingeckoLink: String?
    ) : Parcelable
}
