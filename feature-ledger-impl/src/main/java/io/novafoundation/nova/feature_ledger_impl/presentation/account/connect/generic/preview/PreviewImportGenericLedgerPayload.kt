package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.preview

import android.os.Parcelable
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.common.payload.LedgerGenericAccountParcel
import kotlinx.android.parcel.Parcelize

@Parcelize
class PreviewImportGenericLedgerPayload(
    val account: LedgerGenericAccountParcel,
    val deviceId: String
): Parcelable
