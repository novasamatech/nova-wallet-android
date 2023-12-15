package io.novafoundation.nova.feature_assets.presentation.receive.view

import android.content.Context
import android.os.Bundle
import io.novafoundation.nova.common.view.bottomSheet.ActionNotAllowedBottomSheet
import io.novafoundation.nova.feature_assets.R

class LedgerNotSupportedWarningBottomSheet(
    context: Context,
    onSuccess: () -> Unit,
    private val message: String
) : ActionNotAllowedBottomSheet(context, onSuccess) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        titleView.setText(R.string.assets_receive_ledger_not_supported_title)
        subtitleView.text = message

        applySolidIconStyle(R.drawable.ic_ledger)
    }
}
