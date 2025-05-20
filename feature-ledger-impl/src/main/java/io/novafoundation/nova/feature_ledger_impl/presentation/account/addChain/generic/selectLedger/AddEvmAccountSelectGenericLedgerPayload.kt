package io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.generic.selectLedger

import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.SelectLedgerPayload
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
class AddEvmAccountSelectGenericLedgerPayload(val metaId: Long) : SelectLedgerPayload {

    @IgnoredOnParcel
    override val connectionMode: SelectLedgerPayload.ConnectionMode = SelectLedgerPayload.ConnectionMode.ALL
}
