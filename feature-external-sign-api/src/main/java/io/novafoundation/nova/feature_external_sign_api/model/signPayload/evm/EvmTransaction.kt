package io.novafoundation.nova.feature_external_sign_api.model.signPayload.evm

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class EvmTransaction(
    val gas: String?,
    val gasPrice: String?,
    val from: String,
    val to: String,
    val data: String?,
    val value: String?
) : Parcelable
