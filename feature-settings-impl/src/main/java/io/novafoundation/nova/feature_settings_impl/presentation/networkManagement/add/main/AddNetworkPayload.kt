package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.add.main

import android.os.Parcelable
import java.math.BigInteger
import kotlinx.android.parcel.Parcelize

@Parcelize
class AddNetworkPayload(
    val mode: Mode
) : Parcelable {

    sealed interface Mode : Parcelable {

        @Parcelize
        class Add(val networkType: NetworkType, val prefilledData: NetworkData?) : Mode {

            enum class NetworkType {
                SUBSTRATE, EVM
            }

            @Parcelize
            class NetworkData(
                val iconUrl: String?,
                val isTestNet: Boolean?,
                val rpcNodeUrl: String?,
                val networkName: String?,
                val tokenName: String?,
                val evmChainId: BigInteger?,
                val blockExplorerUrl: String?,
                val coingeckoLink: String?
            ) : Parcelable
        }

        @Parcelize
        class Edit(val chainId: String) : Mode
    }
}
