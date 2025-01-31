package io.novafoundation.nova.feature_external_sign_api.model.signPayload.evm

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class EvmPersonalSignMessage(
    val data: String
) : Parcelable
