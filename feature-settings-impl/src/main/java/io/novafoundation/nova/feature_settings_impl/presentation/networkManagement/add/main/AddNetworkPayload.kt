package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.add.main

import android.os.Parcelable
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.util.ChainParcel
import kotlinx.android.parcel.Parcelize

@Parcelize
class AddNetworkPayload(
    val mode: Mode,

) : Parcelable {

    sealed interface Mode : Parcelable {

        @Parcelize
        class Add(val networkType: NetworkType, val chainParcel: ChainParcel?) : Mode {

            enum class NetworkType {
                SUBSTRATE, EVM
            }
        }

        @Parcelize
        class Edit(val chainId: String) : Mode
    }
}

fun AddNetworkPayload.Mode.Add.getChain(): Chain? {
    return chainParcel?.chain
}
