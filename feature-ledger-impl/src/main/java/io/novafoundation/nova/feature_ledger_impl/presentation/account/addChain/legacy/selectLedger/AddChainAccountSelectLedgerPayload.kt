package io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.legacy.selectLedger

import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.SelectLedgerPayload
import kotlinx.android.parcel.Parcelize

@Parcelize
class AddChainAccountSelectLedgerPayload(
    val addAccountPayload: AddAccountPayload.ChainAccount,
    override val connectionMode: SelectLedgerPayload.ConnectionMode
) : SelectLedgerPayload
