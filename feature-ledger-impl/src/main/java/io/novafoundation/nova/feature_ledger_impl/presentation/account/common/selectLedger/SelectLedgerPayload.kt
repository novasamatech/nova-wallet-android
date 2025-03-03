package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger

import android.os.Parcelable
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.DiscoveryMethod
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.android.parcel.Parcelize

interface SelectLedgerPayload : Parcelable {

    val connectionMode: ConnectionMode

    enum class ConnectionMode {
        BLUETOOTH, USB, ALL
    }
}
