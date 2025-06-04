package io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.generic.selectAddress

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class AddEvmGenericLedgerAccountSelectAddressPayload(
    val metaId: Long,
    val deviceId: String,
) : Parcelable
