package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.finish

import android.os.Parcelable
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.LedgerChainAccount
import kotlinx.android.parcel.Parcelize

@Parcelize
class FinishImportLedgerPayload(
    val ledgerChainAccounts: List<LedgerChainAccount>
) : Parcelable
