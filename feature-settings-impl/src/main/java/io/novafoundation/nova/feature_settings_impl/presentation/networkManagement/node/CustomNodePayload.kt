package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.node

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class CustomNodePayload(
    val chainId: String,
    val mode: Mode
) : Parcelable {

    sealed interface Mode : Parcelable {

        @Parcelize
        object Add : Mode

        @Parcelize
        class Edit(val url: String) : Mode
    }
}
