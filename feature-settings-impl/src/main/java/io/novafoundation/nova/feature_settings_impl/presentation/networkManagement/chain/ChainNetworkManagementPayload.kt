package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.chain

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class ChainNetworkManagementPayload(
    val chainId: String
) : Parcelable
