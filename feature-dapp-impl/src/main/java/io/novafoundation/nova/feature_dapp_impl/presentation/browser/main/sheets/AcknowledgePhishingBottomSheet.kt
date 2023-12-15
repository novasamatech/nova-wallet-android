package io.novafoundation.nova.feature_dapp_impl.presentation.browser.main.sheets

import android.content.Context
import android.os.Bundle
import io.novafoundation.nova.common.view.bottomSheet.ActionNotAllowedBottomSheet
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.main.DappPendingConfirmation

class AcknowledgePhishingBottomSheet(
    context: Context,
    private val confirmation: DappPendingConfirmation<*>,
) : ActionNotAllowedBottomSheet(
    context = context,
    onSuccess = { confirmation.onConfirm() }
) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        titleView.setText(R.string.dapp_phishing_title)
        subtitleView.setText(R.string.dapp_phishing_subtitle)

        applySolidIconStyle(R.drawable.ic_warning_filled)
    }
}
