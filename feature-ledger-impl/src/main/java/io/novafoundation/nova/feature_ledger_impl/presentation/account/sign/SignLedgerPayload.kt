package io.novafoundation.nova.feature_ledger_impl.presentation.account.sign

import android.os.Parcelable
import io.novafoundation.nova.feature_account_api.domain.model.LedgerVariant
import io.novafoundation.nova.feature_account_api.presenatation.sign.SignInterScreenCommunicator
import kotlinx.parcelize.Parcelize

@Parcelize
class SignLedgerPayload(
    val request: SignInterScreenCommunicator.Request,
    val ledgerVariant: LedgerVariant,
) : Parcelable
