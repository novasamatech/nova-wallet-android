package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress

import android.os.Parcelable
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.parcelize.Parcelize

@Parcelize
class SelectLedgerAddressPayload(
    val deviceId: String,
    val chainId: ChainId
) : Parcelable
