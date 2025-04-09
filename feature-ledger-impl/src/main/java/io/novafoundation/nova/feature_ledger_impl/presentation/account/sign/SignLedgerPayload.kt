package io.novafoundation.nova.feature_ledger_impl.presentation.account.sign

import io.novafoundation.nova.feature_account_api.domain.model.LedgerVariant
import io.novafoundation.nova.feature_account_api.presenatation.sign.SignInterScreenCommunicator
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.SelectLedgerPayload
import kotlinx.parcelize.Parcelize

@Parcelize
class SignLedgerPayload(
    val request: SignInterScreenCommunicator.Request,
    val ledgerVariant: LedgerVariant,
    override val connectionMode: SelectLedgerPayload.ConnectionMode,
) : SelectLedgerPayload
