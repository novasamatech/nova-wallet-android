package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.selectAddress

import android.os.Parcelable
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.android.parcel.Parcelize

@Parcelize
class SelectLedgerAddressPayload(
    val deviceId: String,
    val chainId: ChainId
): Parcelable
