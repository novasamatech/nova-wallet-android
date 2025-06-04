package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.finish

import android.os.Parcelable
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.common.payload.LedgerGenericEvmAccountParcel
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.common.payload.LedgerGenericSubstrateAccountParcel
import kotlinx.parcelize.Parcelize

@Parcelize
class FinishImportGenericLedgerPayload(
    val substrateAccount: LedgerGenericSubstrateAccountParcel,
    val evmAccount: LedgerGenericEvmAccountParcel?,
) : Parcelable
