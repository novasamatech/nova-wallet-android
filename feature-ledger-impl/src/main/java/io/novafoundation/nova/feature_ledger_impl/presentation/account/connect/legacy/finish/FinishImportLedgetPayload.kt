package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.finish

import android.os.Parcelable
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.LedgerChainAccount
import kotlinx.android.parcel.Parcelize

@Parcelize
class FinishImportLedgerPayload(
    val ledgerChainAccounts: List<LedgerChainAccount>
) : Parcelable
