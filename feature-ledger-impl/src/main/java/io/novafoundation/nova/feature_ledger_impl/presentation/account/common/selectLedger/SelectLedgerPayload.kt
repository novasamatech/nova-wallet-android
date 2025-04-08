package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger

import android.os.Parcelable

interface SelectLedgerPayload : Parcelable {

    val connectionMode: ConnectionMode

    enum class ConnectionMode {
        BLUETOOTH, USB, ALL
    }
}
