package io.novafoundation.nova.feature_external_sign_api.model.signPayload.evm

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class EvmTransaction : Parcelable {
    @Parcelize
    class Struct(
        val gas: String?,
        val gasPrice: String?,
        val from: String,
        val to: String,
        val data: String?,
        val value: String?,
        val nonce: String?,
    ) : EvmTransaction()

    @Parcelize
    class Raw(val rawContent: String) : EvmTransaction()
}
