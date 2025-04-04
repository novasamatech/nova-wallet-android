package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.fillWallet

import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.SelectLedgerPayload
import kotlinx.android.parcel.Parcelize

@Parcelize
class FillWalletImportLedgerLegacyPayload(override val connectionMode: SelectLedgerPayload.ConnectionMode) : SelectLedgerPayload
