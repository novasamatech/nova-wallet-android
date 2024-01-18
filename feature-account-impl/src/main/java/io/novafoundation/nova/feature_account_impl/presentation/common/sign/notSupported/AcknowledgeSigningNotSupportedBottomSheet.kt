package io.novafoundation.nova.feature_account_impl.presentation.common.sign.notSupported

import android.content.Context
import android.os.Bundle
import io.novafoundation.nova.common.utils.DialogExtensions
import io.novafoundation.nova.common.view.bottomSheet.ActionNotAllowedBottomSheet
import io.novafoundation.nova.feature_account_impl.R

class AcknowledgeSigningNotSupportedBottomSheet(
    context: Context,
    private val payload: SigningNotSupportedPresentable.Payload,
    private val onConfirm: () -> Unit
) : ActionNotAllowedBottomSheet(
    context = context,
    onSuccess = onConfirm,
),
    DialogExtensions {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        titleView.setText(R.string.account_parity_signer_not_supported_title)
        subtitleView.text = payload.message

        applySolidIconStyle(payload.iconRes)
    }
}
