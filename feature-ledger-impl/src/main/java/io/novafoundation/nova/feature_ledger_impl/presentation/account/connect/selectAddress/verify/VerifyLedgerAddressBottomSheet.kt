package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.selectAddress.verify

import android.content.Context
import android.os.Bundle
import io.novafoundation.nova.common.view.bottomSheet.BaseBottomSheet
import io.novafoundation.nova.feature_ledger_impl.R
import kotlinx.android.synthetic.main.fragment_ledger_message.ledgerMessageAction
import kotlinx.android.synthetic.main.fragment_ledger_message.ledgerMessageSubtitle
import kotlinx.android.synthetic.main.fragment_ledger_message.ledgerMessageTitle

class VerifyLedgerAddressBottomSheet(
    context: Context,
    private val onCancel: () -> Unit,
) : BaseBottomSheet(context) {

    init {
        setContentView(R.layout.fragment_ledger_message)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ledgerMessageTitle.setText(R.string.ledger_verify_address_title)

        ledgerMessageAction.setIcon(R.drawable.ic_eye_filled)
        ledgerMessageAction.setMessage(R.string.ledger_verify_address_title)

        ledgerMessageSubtitle.setText(R.string.ledger_verify_address_subtitle)

        setOnCancelListener { onCancel() }
    }
}
