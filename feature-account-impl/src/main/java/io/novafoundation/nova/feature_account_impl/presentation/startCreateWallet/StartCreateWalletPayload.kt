package io.novafoundation.nova.feature_account_impl.presentation.startCreateWallet

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize


@Parcelize
class StartCreateWalletPayload(
    val flowType: FlowType
) : Parcelable {

    enum class FlowType {
        FIRST_WALLET,
        SECOND_WALLET
    }
}
