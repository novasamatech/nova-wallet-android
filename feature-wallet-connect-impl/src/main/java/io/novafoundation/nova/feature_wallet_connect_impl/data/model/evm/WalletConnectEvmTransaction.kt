package io.novafoundation.nova.feature_wallet_connect_impl.data.model.evm

import com.google.gson.annotations.SerializedName

class WalletConnectEvmTransaction(
    val from: String,
    val to: String,
    val data: String?,
    val nonce: String?,
    val gasPrice: String?,
    @SerializedName("gasLimit", alternate = ["gas"])
    val gasLimit: String?,
    val value: String?,
)
