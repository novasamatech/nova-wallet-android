package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.finish

import android.os.Parcelable
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.common.payload.LedgerGenericAccountParcel
import kotlinx.parcelize.Parcelize

@Parcelize
class FinishImportGenericLedgerPayload(
    val account: LedgerGenericAccountParcel
) : Parcelable
