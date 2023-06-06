package io.novafoundation.nova.feature_external_sign_api.model.signPayload.evm

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class EvmTypedMessage(
    val data: String,
    val raw: String?
) : Parcelable
