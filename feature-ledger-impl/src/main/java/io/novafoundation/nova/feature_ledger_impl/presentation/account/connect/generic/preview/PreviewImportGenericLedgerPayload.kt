package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.preview

import android.os.Parcelable
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.common.payload.LedgerGenericAccountParcel
import kotlinx.parcelize.Parcelize

@Parcelize
class PreviewImportGenericLedgerPayload(
    val accountIndex: Int,
    val account: LedgerGenericAccountParcel,
    val deviceId: String
) : Parcelable
